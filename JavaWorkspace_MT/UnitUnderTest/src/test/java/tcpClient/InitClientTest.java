package tcpClient;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import tcpServer.ComputeEngine_Runnable;
import tcpServer.TCPserver;
import tcpServer.TCPserver_Teardown;
import watchdog.Local_1h_Watchdog;

public class InitClientTest {

	int port_1 = 9876;
	int sensor_ID_1 = 1;
	TCPclient tcpclient_1, tcpclient_4 = null;
	int port_2 = 9889;
	int sensor_ID_2 = 2;
	TCPclient tcpclient_2, tcpclient_5 = null;
	int sensor_ID_3 = 3;
	TCPclient tcpclient_3, tcpclient_6 = null;
	String serverHostName = "localhost";
	
	// to mock TCPserver instances with ServerSocket mocks
	TCPserver mockTCPserverTest = null;
	ServerSocket tempServerSocket_1 = null;
	ServerSocket tempServerSocket_2 = null;
	
	// to mock Client Socket instance in the mockClientSocket = servSocket.accept() statement
	Socket mockClientSocket = null;
	
	// to mock server threads
	Thread mockServerThread = null;
	ComputeEngine_Runnable mockComputeEngine_Runnable = null;
	ExecutorService executor = Executors.newSingleThreadExecutor();
	private final ThreadPoolExecutor auxiliaryServerThreadExecutor = new ThreadPoolExecutor(8, 8, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
	
	String[] testPurpose = { 	"Verify that the Connect Exception is returned if there was an attempt to create the client without a server instance initialized previously",
								"Verify that once the server is initialized at any registered port, the client can be created successfully at this port",
								"Verify that once the server is initialized at any registered port, multiple client instances can be created successfully at the same port",
								"Verify that once multiple server instances are initialized at any different registered ports, multiple client instances can be created successfully at any port with a server instance initialized",
								"Verify that the Connect Exception is returned if there was an attempt to create the client at an invalid address on local machine, or port that is not valid on remote machine",
								"Verify that the Connect Exception caused by connection timeout is returned if there was an attempt to create the client at the IP address of the host, for which the connection cannot be established",
								//"Verify that the Unknkown Host Exception is returned if there was an attempt to create the client at the IP address of the host that can not be determined"
								};
								
	static int testID = 1;
	
	public static void incrementTestID() {
		InitClientTest.testID += 1;
	}
	
	
	@Before
	public void before() throws IOException {
		tcpclient_1 = new TCPclient();

		if(InitClientTest.testID > 1 ) {
			// mock Server Socket to enable the Client Socket to establish connection
			mockTCPserverTest = mock(TCPserver.class);
			mockClientSocket = mock(Socket.class);
			
			tempServerSocket_1 = new ServerSocket();
			tempServerSocket_2 = new ServerSocket();
			
			when(mockTCPserverTest.getServerSocket()).thenReturn(tempServerSocket_1); 

			/* To avoid "remote deadlock" - there is a need to submit mockComputeEngine_Runnable to ThreadPoolExecutor 
			 * The ObjectInputStream on the client is waiting for the object stream from the server before proceeding, but the server isn't going to send that, 
			 * because its ObjectInputStream is waiting for the header from the client before proceeding... 
			 */
			
			Mockito.doAnswer(new Answer<Thread>() {
	            @Override
	            public Thread answer(InvocationOnMock invocation) throws Throwable {
	                Object[] arguments = invocation.getArguments();
	                if (arguments != null && arguments.length > 0 && arguments[0] != null ) {
	                	final ServerSocket servSocket = (ServerSocket) arguments[0];
	                	mockServerThread = new Thread(new Runnable() {
	                		public void run() {
	                			while(!servSocket.isClosed()) {
			                		try {
										mockClientSocket = servSocket.accept();
										mockComputeEngine_Runnable = Mockito.spy(new ComputeEngine_Runnable(mockClientSocket, 1.0, false));
										auxiliaryServerThreadExecutor.submit(mockComputeEngine_Runnable);
									} catch (IOException IOex) {
										mockServerThread.interrupt();
										System.out.println("Server Thread Stopped.");
										System.out.println("Server" + IOex.getMessage());
										break;
									}
	                			}
	                		}
	                	});
	                }
	                return mockServerThread;
	            }
			}).when(mockTCPserverTest).startServer(Matchers.any(ServerSocket.class));

		// Mockito.doAnswer - to mock void method to do something (mock the behavior despite being void) - in this case it is used for TCPserver.startServer();
		// the test uses this approach for the purpose of avoiding actual messages sent via TCP - it will be checked in the integration tests
		}
		
		System.out.println("\t\tTest Run "+InitClientTest.testID+" Purpose:");
		System.out.println(testPurpose[(InitClientTest.testID-1)]);
		System.out.println("\t\tTest Run "+InitClientTest.testID+" Logic:");
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_1
	 * Description: 			Verify that the Connect Exception is returned if there was an attempt to create the client without a server instance initialized previously
	 * Exceptions thrown TBV:	ConnectException
     * Exceptions thrown: 		IOException
	 ***********************************************************************************************************/
	@Test(expected = ConnectException.class)
	public void test_run_1() throws IOException {
		
		tcpclient_1 = tcpclient_1.initClient(sensor_ID_1, serverHostName, port_1);
		
		// To prove that exception's stack trace reported by JUnit caught the ConnectException
		assertTrue(false);
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_2
	 * Description: 			Verify that once the server is initialized at any registered port, the client can be created successfully at this port
	 * Internal variables TBV: 	clientRunning
	 * Mocked external methods: TCPserver.startServer()
     * Exceptions thrown: 		IOException, InterruptedException
	 ***********************************************************************************************************/
	@Test
	public void test_run_2() throws IOException, InterruptedException {
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tcpclient_1 = tcpclient_1.initClient(sensor_ID_1, serverHostName, port_1);
		assertTrue(tcpclient_1.isClientRunning());
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_3
	 * Description: 			Verify that once the server is initialized at any registered port, multiple client instances can be created successfully at the same port
	 * Internal variables TBV: 	clientRunning
	 * Mocked external methods: TCPserver.startServer()
     * Exceptions thrown: 		IOException, InterruptedException
	 ***********************************************************************************************************/
	@Test
	public void test_run_3() throws IOException {
		
		tcpclient_2 = new TCPclient();
		tcpclient_3 = new TCPclient();
		tcpclient_4 = new TCPclient();
		tcpclient_5 = new TCPclient();
		tcpclient_6 = new TCPclient();
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tcpclient_1 = tcpclient_1.initClient(sensor_ID_1, serverHostName, port_1);
		tcpclient_2 = tcpclient_2.initClient(sensor_ID_2, serverHostName, port_1);
		tcpclient_3 = tcpclient_3.initClient(sensor_ID_3, serverHostName, port_1);
		
		assertTrue(tcpclient_1.isClientRunning());
		assertTrue(tcpclient_2.isClientRunning());
		assertTrue(tcpclient_3.isClientRunning());
		
	}

    /***********************************************************************************************************
	 * Test Name: 				test_run_4
	 * Description: 			Verify that once multiple server instances are initialized at any different registered ports, 
	  							multiple client instances can be created successfully at any port with a server instance initialized
	 * Internal variables TBV: 	clientRunning
	 * Mocked external methods: TCPserver.startServer()
     * Exceptions thrown: 		IOException, InterruptedException
	 ***********************************************************************************************************/
	@Test
	public void test_run_4() throws IOException, InterruptedException {
		
		when(mockTCPserverTest.getServerSocket()).thenReturn(tempServerSocket_1).thenReturn(tempServerSocket_1).thenReturn(tempServerSocket_2).thenReturn(tempServerSocket_2); 
		
		tcpclient_2 = new TCPclient();
		tcpclient_3 = new TCPclient();
		tcpclient_4 = new TCPclient();
		tcpclient_5 = new TCPclient();
		tcpclient_6 = new TCPclient();
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_2));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
	
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket()); 
		mockServerThread.start();
		
		tcpclient_1 = tcpclient_1.initClient(sensor_ID_1, serverHostName, port_1);
		tcpclient_2 = tcpclient_2.initClient(sensor_ID_2, serverHostName, port_1);
		tcpclient_3 = tcpclient_3.initClient(sensor_ID_3, serverHostName, port_1);
		
		assertTrue(tcpclient_1.isClientRunning());
		assertTrue(tcpclient_2.isClientRunning());
		assertTrue(tcpclient_3.isClientRunning());

		
		tcpclient_1 = tcpclient_1.initClient(sensor_ID_1, serverHostName, port_2);
		tcpclient_2 = tcpclient_2.initClient(sensor_ID_2, serverHostName, port_2);
		tcpclient_3 = tcpclient_3.initClient(sensor_ID_3, serverHostName, port_2);
		
		assertTrue(tcpclient_1.isClientRunning());
		assertTrue(tcpclient_2.isClientRunning());
		assertTrue(tcpclient_3.isClientRunning());
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_5
	 * Description: 			Verify that the Connect Exception is returned 
	 							if there was an attempt to create the client at an invalid address on local machine, or port that is not valid on remote machine
	 * Exceptions thrown TBV:	ConnectException
     * Exceptions thrown: 		IOException
	 ***********************************************************************************************************/
	@Test(expected = ConnectException.class)
	public void test_run_5() throws IOException {
		
		when(mockTCPserverTest.getServerSocket()).thenReturn(tempServerSocket_1);
		
		serverHostName = "1.1.1.1";

		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket()); 
		mockServerThread.start();
		
		// serverHostName = "1.1.1.1";
		tcpclient_1 = tcpclient_1.initClient(sensor_ID_1, serverHostName, port_1);
		
		// To prove that exception's stack trace reported by JUnit caught the ConnectException
		assertTrue(false);
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_6
	 * Description: 			Verify that the Connect Exception caused by connection timeout is returned 
	 							if there was an attempt to create the client at the IP address of the host, for which the connection cannot be established
	 * Exceptions thrown TBV:	ConnectException
     * Exceptions thrown: 		IOException
	 ***********************************************************************************************************/
	@Test(expected = ConnectException.class)
	public void test_run_6() throws IOException {
		
		port_1 = 0;

		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket()); 
		mockServerThread.start();
		
		// port_1 = 0
		tcpclient_1 = tcpclient_1.initClient(sensor_ID_1, serverHostName, port_1);
		
		// To prove that exception's stack trace reported by JUnit caught the ConnectException
		assertTrue(false);
	}
	
   @After
    public void teardown() throws IOException, InterruptedException{
	  
	   System.out.println("\t\tTest Run "+InitClientTest.testID+" teardown section:");
	   
	   if(tcpclient_1 != null) {
		   if(tcpclient_1.isClientRunning()){
			   tcpclient_1.closeClient(tcpclient_1);
		   }
	   }
	   if(tcpclient_2 != null){
		   if(tcpclient_2.isClientRunning()){
			   tcpclient_2.closeClient(tcpclient_2);
		   }
	   }
	   if(tcpclient_3 != null){
		   if(tcpclient_3.isClientRunning()){
			   tcpclient_3.closeClient(tcpclient_3);
		   }
	   }
	   if(tcpclient_4 != null) {
		   if(tcpclient_1.isClientRunning()){
			   tcpclient_1.closeClient(tcpclient_1);
		   }
	   }
	   if(tcpclient_5 != null){
		   if(tcpclient_2.isClientRunning()){
			   tcpclient_2.closeClient(tcpclient_2);
		   }
	   }
	   if(tcpclient_6 != null){
		   if(tcpclient_3.isClientRunning()){
			   tcpclient_3.closeClient(tcpclient_3);
		   }
	   }
	   if(Local_1h_Watchdog.getInstance() != null) {
		   Local_1h_Watchdog.getInstance().setM_instance(null);
	   }
	   
	   // run the reinitalize_to_default() function that sets all attributes of a static class TCPserver to default
	   TCPserver_Teardown tcp_server_teardown = new TCPserver_Teardown();
	   tcp_server_teardown.reinitalize_to_default(mockTCPserverTest);

	   // Time offset between consecutive test runs execution
	   Thread.sleep(100);
	   
	   System.out.println("");
	   incrementTestID();
    }
	
}


