package pt.ulisboa.tecnico.cnv.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;
import pt.ulisboa.tecnico.cnv.solver.Solver;
import pt.ulisboa.tecnico.cnv.solver.SolverArgumentParser;
import pt.ulisboa.tecnico.cnv.solver.SolverFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class WebServer {

	private static Map<Long, Integer> i_count = new HashMap<>();
	private static Map<Long, Integer> b_count = new HashMap<>();
	private static Map<Long, Integer> m_count = new HashMap<>();

	public static void main(final String[] args) throws Exception {

		final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8000), 0);

		//final HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

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
	static class MyHandler implements HttpHandler {
		@Override
		public void handle(final HttpExchange t) throws IOException {

			// Get the query.
			final String query = t.getRequestURI().getQuery();
			System.out.println("> Query:\t" + query);

			// Break it down into String[].
			final String[] params = query.split("&");

			final ArrayList<String> newArgs = new ArrayList<>();
			// print type of request into intrumentation file
			try {
				FileWriter myWriter = new FileWriter("requests_instrumentation.txt", true);

				// Store as if it was a direct call to SolverMain.
				for (final String p : params) {
					final String[] splitParam = p.split("=");
					newArgs.add("-" + splitParam[0]);
					newArgs.add(splitParam[1]);

					// write puzzle params into file
					myWriter.write(" - " + splitParam[1]);
				}

				myWriter.write("\n");
				myWriter.close();

			} catch (IOException e) {
				System.out.println("An error occurred.");
				e.printStackTrace();
			}


			newArgs.add("-b");
			newArgs.add(parseRequestBody(t.getRequestBody()));

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


			// Send response to browser.
			final Headers hdrs = t.getResponseHeaders();

            //t.sendResponseHeaders(200, responseFile.length());


			///hdrs.add("Content-Type", "image/png");
            hdrs.add("Content-Type", "application/json");

			hdrs.add("Access-Control-Allow-Origin", "*");

            hdrs.add("Access-Control-Allow-Credentials", "true");
			hdrs.add("Access-Control-Allow-Methods", "POST, GET, HEAD, OPTIONS");
			hdrs.add("Access-Control-Allow-Headers", "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers");

            t.sendResponseHeaders(200, solution.toString().length());


            final OutputStream os = t.getResponseBody();
            OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
            osw.write(solution.toString());
            osw.flush();
            osw.close();

			os.close();

			System.out.println("> Sent response to " + t.getRemoteAddress().toString());

			printICountToFile();
		}
	}

	public static synchronized void printICountToFile() {
		Long currentThreadId = Thread.currentThread().getId();
		try {
			FileWriter myWriter = new FileWriter("requests_instrumentation.txt", true);

			// print results and put counters back to zero
			myWriter.write(i_count.get(currentThreadId) + " instructions in thread" + currentThreadId + '\n');
			i_count.put(currentThreadId, 0);

			myWriter.write(b_count.get(currentThreadId) + " basic blocks in thread" + currentThreadId + '\n');
			b_count.put(currentThreadId, 0);

			myWriter.write(m_count.get(currentThreadId) + " methods in thread" + currentThreadId + '\n');
			m_count.put(currentThreadId, 0);

			myWriter.write("-------------------------------------------------" + '\n');
			myWriter.close();

		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	}

	public static synchronized void count(int incr) {
		Long currentThreadId = Thread.currentThread().getId();
		Integer currentICount = i_count.get(currentThreadId);
		if (currentICount == null) currentICount = 0;
		Integer currentBCount = b_count.get(currentThreadId);
		if (currentBCount == null) currentBCount = 0;

		i_count.put(currentThreadId, currentICount + incr);
		b_count.put(currentThreadId, ++currentBCount);
	}

	public static synchronized void mcount(int incr) {
		Long currentThreadId = Thread.currentThread().getId();
		Integer currentMCount = m_count.get(currentThreadId);
		if (currentMCount == null) currentMCount = 0;
		m_count.put(currentThreadId, ++currentMCount);
	}
}
