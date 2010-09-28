package net.sourceforge.squirrel_sql.plugins.hibernate;


public interface IHibernateConnectionProvider
{
   HibernateConnection getHibernateConnection();

   void addConnectionListener(ConnectionListener connectionListener);
}
