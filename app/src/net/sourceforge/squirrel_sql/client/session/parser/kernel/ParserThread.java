/*
 * Copyright (C) 2002 Christian Sell
 * csell@users.sourceforge.net
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * created by cse, 07.10.2002 11:57:54
 *
 * @version $Id: ParserThread.java,v 1.3 2004-11-27 23:39:55 gerdwagner Exp $
 */
package net.sourceforge.squirrel_sql.client.session.parser.kernel;

import net.sourceforge.squirrel_sql.client.session.parser.kernel.completions.ErrorListener;
import net.sourceforge.squirrel_sql.client.session.parser.kernel.completions.SQLSelectStatementListener;
import net.sourceforge.squirrel_sql.client.session.parser.kernel.completions.SQLStatement;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Vector;

/**
 * a thread subclass which drives the SQL parser. The thread reads from a _workingBuffer
 * which always blocks until data is made available. It can thus be run in the
 * background, parsing the input from the text UI as it arrives.
 *
 * <em>Unfortunately, it depends on the generated parser/scanner and therefore
 * cannot be generalized, unless the generated classes are made to implement public
 * interfaces</em>
 */
public class ParserThread extends Thread
{
	public static final String PARSER_THREAD_NM = "SQLParserThread";

	private String _pendingString;
	private Errors _errors;
	private SQLSchema _schema;
	private SQLStatement _curSQLSelectStat;


	private Vector _workingTableAliasInfos = new Vector();
	private TableAliasInfo[] _lastRunTableAliasInfos = new TableAliasInfo[0];
	private Vector _workingErrorInfos = new Vector();
	private ErrorInfo[] _lastRunErrorInfos = new ErrorInfo[0];


	private boolean _exitThread;
	private ParsingFinishedListener _parsingFinishedListener;

	private int _lastParserRunOffset;
	private int _lastErrEnd = -1;
   private int _nextStatBegin = -1;

	private String _workingString;
	private IncrementalBuffer _workingBuffer;
	private boolean _errorDetected;

   public ParserThread(SQLSchema schema)
	{
		super(PARSER_THREAD_NM);
      this._schema = schema;

		ErrorListener errListener = new ErrorListener()
		{
			public void errorDetected(String message, int line, int column)
			{
				onErrorDetected(message, line, column);
			}
		};


		this._errors = new Errors(errListener);

      setPriority(Thread.MIN_PRIORITY);

		start();
	}

	private void onErrorDetected(String message, int line, int column)
	{
		_errorDetected = true;
		int errPos = getPos(line, column);
		_lastErrEnd = getTokenEnd(errPos);
      _nextStatBegin = predictNextStatementBegin(errPos);

      int beginPos = _lastParserRunOffset + errPos;
      int endPos = _lastParserRunOffset + _lastErrEnd;

      if(beginPos < endPos)
      {
         _workingErrorInfos.add(new ErrorInfo(message, _lastParserRunOffset + errPos-1 , _lastParserRunOffset + _lastErrEnd-1));
      }
	}

   private int predictNextStatementBegin(int errPos)
   {
      int ret = errPos;
      while(   _workingString.length() > ret && false == startsWithBeginKeyWord(ret) )
      {
         ++ret;
      }
      return ret;
   }

   private boolean startsWithBeginKeyWord(int ret)
   {
      return    startsWithIgnoreCase(ret, "SELECT")
             || startsWithIgnoreCase(ret, "UPDATE")
             || startsWithIgnoreCase(ret, "DELETE")
             || startsWithIgnoreCase(ret, "INSERT")
             || startsWithIgnoreCase(ret, "ALTER")
             || startsWithIgnoreCase(ret, "CREATE")
             || startsWithIgnoreCase(ret, "DROP");
   }

   private boolean startsWithIgnoreCase(int ret, String keyWord)
   {
      int beginPos;
      int endPos;

      if(ret == 0)
      {
         beginPos = 0;
      }
      else if(Character.isWhitespace(_workingString.charAt(ret)))
      {
         beginPos = ret + 1;
      }
      else
      {
         return false;
      }

      if(_workingString.length() == beginPos + keyWord.length())
      {
         endPos = beginPos + keyWord.length();
      }
      else if(_workingString.length() > beginPos + keyWord.length() && Character.isWhitespace(_workingString.charAt(beginPos + keyWord.length())))
      {
         endPos = beginPos + keyWord.length();
      }
      else
      {
         return false;
      }

      return keyWord.equalsIgnoreCase(_workingString.substring(beginPos, endPos));
   }


   private int getTokenEnd(int errPos)
	{
		int ret = errPos;
		while(_workingString.length() > ret && false == Character.isWhitespace(_workingString.charAt(ret)))
		{
			++ret;
		}
		return ret;
	}


	private int getPos(int line, int column)
	{
		int ix = 0;

		///////////////////////////////////////////
		// find the place where the error occured
		for (int i = 0; i < line-1; i++)
		{
			ix =_workingString.indexOf('\n', ix) + 1;
		}
		ix += column;
		//
		///////////////////////////////////////////
		return ix;
	}

	public void notifyParser(String sqlText)
	{
		synchronized(this)
		{
			_pendingString = sqlText;
			this.notify();
		}
	}

	public void exitThread()
	{
		_exitThread = true;
		synchronized(this)
		{
			this.notify();
		}
	}

	public void setParsingFinishedListener(ParsingFinishedListener parsingFinishedListener)
	{
		_parsingFinishedListener = parsingFinishedListener;
	}


	public void run()
	{
		try
		{
			while(true)
			{
				synchronized(this)
				{
					this.wait();
					_workingString = _pendingString;
					_workingBuffer = new IncrementalBuffer(new StringCharacterIterator(_workingString));
				}

				if(_exitThread)
				{
					break;
				}

				//////////////////////////////////////////////////////////////
				// On Errors we restart the parser behind the error
				_errorDetected = false;
				runParser();
				while(_errorDetected)
				{
					if(_workingString.length() > _nextStatBegin)
					{
						_workingString = _workingString.substring(_nextStatBegin, _workingString.length());
						if("".equals(_workingString.trim()))
						{
							break;
						}
					}
					else
					{
						break;
					}

//					if(_workingString.length() > _lastErrEnd)
//					{
//						_workingString = _workingString.substring(_lastErrEnd, _workingString.length());
//						if("".equals(_workingString.trim()))
//						{
//							break;
//						}
//					}
//					else
//					{
//						break;
//					}

					_lastParserRunOffset += _nextStatBegin;
					_workingBuffer = new IncrementalBuffer(new StringCharacterIterator(_workingString));

					_errorDetected = false;
					runParser();
				}

				//
				////////////////////////////////////////////////////////////


				///////////////////////////////////////////////////////////
				// We are through with parsing. Now we store the outcome
				// in _lastRun... and tell the listeners.
				_lastRunTableAliasInfos = (TableAliasInfo[]) _workingTableAliasInfos.toArray(new TableAliasInfo[_workingTableAliasInfos.size()]);
				_lastRunErrorInfos = (ErrorInfo[]) _workingErrorInfos.toArray(new ErrorInfo[_workingErrorInfos.size()]);
				_workingTableAliasInfos.clear();
				_workingErrorInfos.clear();
				_lastParserRunOffset = 0;
				if(null != _parsingFinishedListener)
				{
					_parsingFinishedListener.parsingFinished();
				}
				//
				/////////////////////////////////////////////////////////////

				if(_exitThread)
				{
					break;
				}
			}
		}
		catch (Exception e)
		{
			if(null != _parsingFinishedListener)
			{
				_parsingFinishedListener.parserExitedOnException(e);
			}
			e.printStackTrace();
		}
	}

	private void runParser()
	{
		_errors.reset();
		Scanner scanner = new Scanner(_workingBuffer, _errors);

		Parser parser = new Parser(scanner);
		parser.rootSchema = _schema;

		parser.addParserListener(new ParserListener()
		{
			public void statementAdded(SQLStatement statement)
			{
				onStatementAdded(statement);
			}
		});

		parser.addSQLSelectStatementListener(new SQLSelectStatementListener()
		{
			public void aliasDefined(String tableName, String aliasName)
			{
				onAliasDefined(tableName, aliasName);
			}
		});


		parser.parse();
	}

	private void onStatementAdded(SQLStatement statement)
	{
		_curSQLSelectStat = statement;
	}

	private void onAliasDefined(String tableName, String aliasName)
	{
		_workingTableAliasInfos.add(new TableAliasInfo(aliasName, tableName, _curSQLSelectStat.getStart() + _lastParserRunOffset));
	}

	public TableAliasInfo[] getTableAliasInfos()
	{
		return _lastRunTableAliasInfos;
	}

	public ErrorInfo[] getErrorInfos()
	{
		return _lastRunErrorInfos;
	}

	/**
	 * reset the parser, starting a new parse on the characters given by the iterator
	 * @param chars the characters to start parsing from
	 */
	public void reset(CharacterIterator chars)
	{
		IncrementalBuffer oldBuffer = this._workingBuffer;
		this._workingBuffer = new IncrementalBuffer(chars);
		oldBuffer.eof();
	}

	/**
	 * terminate the parser
	 */
	public void end()
	{
		IncrementalBuffer oldBuffer = this._workingBuffer;
		this._workingBuffer = null;
		oldBuffer.eof();
	}

	/**
	 * accept the next character sequence to be parsed
	 * @param chars
	 */
	public void accept(CharacterIterator chars)
	{
		_workingBuffer.waitChars();     //wait for pending chars to be processed
		_workingBuffer.accept(chars);   //post new characters
	}

	/**
	 * This is a Scanner.Buffer implementation which blocks until character data is
	 * available. The {@link #read} method is invoked from the background parsing thread.
	 * The parsing thread can be terimated by calling the {@link #eof} method on this object
	 */
	private static class IncrementalBuffer extends Scanner.Buffer
	{
		private CharacterIterator chars;
		private char current;
		private boolean atEnd;

		IncrementalBuffer(CharacterIterator chars)
		{
			this.atEnd = false;
			this.chars = chars;
			this.current = chars != null ? chars.first() : CharacterIterator.DONE;
		}

		/**
		 * read the next character. This method is invoked from the parser thread
		 * @return the next available character
		 */
		protected synchronized char read()
		{
			if (atEnd)
			{
				return eof;
			}
			else
			{
				if (current == CharacterIterator.DONE)
				{
					if (chars != null)
					{
						synchronized (chars)
						{
							chars.notify(); //tell the UI that this _workingBuffer is through
						}
					}
//					try
//					{
//						wait();
//					}
//					catch (InterruptedException e)
//					{
//					}
				}
				if (atEnd)
				{
					current = eof;
					return eof;
				}
				else
				{
					char prev = current;
					//System.out.print(prev);
					current = chars.next();
					return prev;
				}
			}
		}

		synchronized void eof()
		{
			atEnd = true;
			notify();
		}

		/**
		 * Post a character sequence to be read. Notify the parser thread accordingly. Invoking
		 * this method should always be followed by a call to {@link #waitChars} to ensure that
		 * the character sequence is not overwritten before it has been fully processed.
		 * @param chars the chracters to be read
		 */
		synchronized void accept(CharacterIterator chars)
		{
			this.chars = chars;
			this.current = chars != null ? chars.first() : CharacterIterator.DONE;
			notify();
		}

		/**
		 * block the current thread until all characters from the current iterator have
		 * been processed
		 */
		void waitChars()
		{
			if (chars != null && current != CharacterIterator.DONE)
			{
				synchronized (chars)
				{
					try
					{
						chars.wait();
					}
					catch (InterruptedException e)
					{
					}
				}
			}
		}

		int getBeginIndex()
		{
			return chars != null ? chars.getBeginIndex() : 0;
		}
	}

	/**
	 * error stream which simply saves the error codes and line info
	 * circularily in an array of fixed size, and notifies a listener
	 * if requested
	 */
	private static class Errors extends ErrorStream
	{
		private int[][] errorStore;
		private int count;
		private ErrorListener listener;

		public Errors(ErrorListener listener)
		{
			this.listener = listener;
			errorStore = new int[5][3];
		}

		protected void ParsErr(int n, int line, int col)
		{
			errorStore[count][0] = n;
			errorStore[count][1] = line;
			errorStore[count][2] = col;
			count = (count + 1) % 5;
			if (listener != null)
				super.ParsErr(n, line, col);
		}

		protected void SemErr(int n, int line, int col)
		{
			errorStore[count][0] = n;
			errorStore[count][1] = line;
			errorStore[count][2] = col;
			count = (count + 1) % 5;
			if (listener != null)
			{
				switch (n)
				{
					case 10:
						StoreError(n, line, col, "undefined table");
						break;
					default:
						super.SemErr(n, line, col);
				}
			}
		}

		protected void StoreError(int n, int line, int col, String s)
		{
			if (listener != null)
				listener.errorDetected(s, line, col);
		}

		public void reset()
		{
			errorStore = new int[5][3];
		}
	}
}
