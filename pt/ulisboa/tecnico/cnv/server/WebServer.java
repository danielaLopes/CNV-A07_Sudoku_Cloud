package pt.ulisboa.tecnico.cnv.server;

import BIT.serverMetrics.*;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;
import pt.ulisboa.tecnico.cnv.solver.Solver;
import pt.ulisboa.tecnico.cnv.solver.SolverArgumentParser;
import pt.ulisboa.tecnico.cnv.solver.SolverFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class WebServer {

	public static void main(final String[] args) throws Exception {

		final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8000), 0);

		//final HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
		server.createContext("/ping", new MyPingHandler());
		server.createContext("/sudoku", new MyHandler());

		// be aware! infinite pool of threads!
		server.setExecutor(Executors.newCachedThreadPool());
		server.start();

		System.out.println(server.getAddress().toString());
	}


	public static String parseRequestBody(InputStream is) throws IOException {
        InputStreamReader isr =  new InputStreamReader(is,"utf-8");
        BufferedReader br = new BufferedReader(isr);

        // From now on, the right way of moving from bytes to utf-8 characters:

        int b;
        StringBuilder buf = new StringBuilder(512);
        while ((b = br.read()) != -1) {
            buf.append((char) b);

        }

        br.close();
        isr.close();

        return buf.toString();
	}
	
	static class MyPingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {

            System.out.println("> Received PING");

            String response = "This was the query:" + t.getRequestURI().getQuery() 
                               + "##";
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
	}
	
	static class MyHandler implements HttpHandler {
		@Override
		public void handle(final HttpExchange t) throws IOException {

			// Get the query.
			final String query = t.getRequestURI().getQuery();
			System.out.println("> Query:\t" + query);

			// Break it down into String[].
			final String[] params = query.split("&");

			final ArrayList<String> newArgs = new ArrayList<>();

			// Store as if it was a direct call to SolverMain.
			for (final String p : params) {
				final String[] splitParam = p.split("=");
				newArgs.add("-" + splitParam[0]);
				newArgs.add(splitParam[1]);
			}

			String body = parseRequestBody(t.getRequestBody());

			System.out.println("body size: " + body.length());

			newArgs.add("-b");
			newArgs.add(body);
			newArgs.add("-d");

			// Store from ArrayList into regular String[].
			final String[] args = new String[newArgs.size()];
			int i = 0;
			for(String arg: newArgs) {
				args[i] = arg;
				i++;
			}

			// Get user-provided flags.
			final SolverArgumentParser ap = new SolverArgumentParser(args);

			// Create solver instance from factory.
			final Solver s = SolverFactory.getInstance().makeSolver(ap);

			//Solve sudoku puzzle
			JSONArray solution = s.solveSudoku();

			// send response to /response endpoint from LoadBalancer after solving sudoku
			final Headers hdrs = t.getResponseHeaders();

			hdrs.add("Content-Type", "application/json");

			hdrs.add("Access-Control-Allow-Origin", "*");

			hdrs.add("Access-Control-Allow-Credentials", "true");
			hdrs.add("Access-Control-Allow-Methods", "POST, GET, HEAD, OPTIONS");
			hdrs.add("Access-Control-Allow-Headers", "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers");

			Long fieldLoads = MetricsTool.insertDynamo(query);
			// TODO: see if it's worthed to store these in a buffer
			System.out.println("inserted data to dynamo");

			String response = solution.toString() + ":" + fieldLoads.toString();

			t.sendResponseHeaders(200, response.length());

			final OutputStream os = t.getResponseBody();
			OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");

			osw.write(response);
			osw.flush();
			osw.close();

			os.close();
			System.out.println("> Sent response to " + t.getRemoteAddress().toString());

			//MetricsTool.printToFile(query);
			System.out.flush();
		}
	}
}
