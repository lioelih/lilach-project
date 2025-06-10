package il.cshaifasweng.OCSFMediatorExample.server;

import java.io.IOException;
import il.cshaifasweng.OCSFMediatorExample.server.HibernateUtil;

/**
 * Hello world!
 *
 */
public class App 
{
	
	private static SimpleServer server;
    public static void main( String[] args ) throws IOException
    {
        HibernateUtil.printAllProducts();
        server = new SimpleServer(3000);
        server.listen();

    }
}
