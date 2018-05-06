package model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Port;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.result.WordResult;
import edu.cmu.sphinx.util.LogMath;

public class SpeechRecognizerMain {

    // Necessary
    private static LiveSpeechRecognizer recognizer;
    private static LiveSpeechRecognizer recognizerEng;
    private static LiveSpeechRecognizer recognizerViet;
    private static LiveSpeechRecognizer recognizerJap;
    private static BufferedReader in;
    private static PrintWriter out;
    private static String inputline;
    // Logger
    private Logger logger = Logger.getLogger(getClass().getName());

    /**
     * This String contains the Result that is coming back from SpeechRecognizer
     */
    private static String speechRecognitionResult;

    private final static String VIETNAMESE = "vi";
    private final static String ENGLISH = "eng";
    private final static String JAPANESE = "jp";

    private final static String RUNNING = "running";
    private final static String BUSY = "busy";

    private static String nlpAgentState = BUSY;
    private static String nlpAgentLanguage = ENGLISH;
    
    private static String INITSYS = "INITSYS";
    private static Object syncObject = new Object();

    private static boolean flag = false;

    // -----------------Lock Variables-----------------------------

    /**
     * This variable is used to ignore the results of speech recognition cause
     * actually it can't be stopped...
     * 
     * <br>
     * Check this link for more information: <a href=
     * "https://sourceforge.net/p/cmusphinx/discussion/sphinx4/thread/3875fc39/"
     * > https://sourceforge.net/p/cmusphinx/discussion/sphinx4/thread/3875fc39/
     * </a>
     */
    private static boolean ignoreSpeechRecognitionResults = false;

    /**
     * Checks if the speech recognise is already running
     */
    private static boolean speechRecognizerThreadRunning = false;

    /**
     * Checks if the resources Thread is already running
     */
    private static boolean resourcesThreadRunning;

    /**
     * Checks if the resources Thread is already running
     */
    private static boolean clientThreadRunning;

    private static boolean changedLanguage = false;

    private static Configuration configuration;
    // ---

    /**
     * This executor service is used in order the playerState events to be
     * executed in an order
     */
    private static ExecutorService eventsExecutorService = Executors.newFixedThreadPool(3);

    // ------------------------------------------------------------------------------------

    /**
     * Constructor
     * 
     * @throws IOException
     */
    public SpeechRecognizerMain() throws IOException {
	// Configuration
	configuration = new Configuration();

	// Start recognition process pruning previously cached data.
	// recognizer.startRecognition(true);
//	try {
//	    configEnglish();
//	    recognizerEng = new LiveSpeechRecognizer(configuration);
//	} catch (IOException ex) {
//	    // logger.log(Level.SEVERE, null, ex);
//	    ex.printStackTrace();
//	}
//	try {
//	    configVietnamese();
//	    recognizerViet = new LiveSpeechRecognizer(configuration);
//	} catch (IOException ex) {
//	    // logger.log(Level.SEVERE, null, ex);
//	    ex.printStackTrace();
//	}
	// try {
	// configJapanese();
	// recognizerJap = new LiveSpeechRecognizer(configuration);
	// } catch (IOException ex) {
	// // logger.log(Level.SEVERE, null, ex);
	// ex.printStackTrace();
	// }

	// Check if needed resources are available
	startResourcesThread();
	// Connect to server
	clientSocket();
	// Start speech recognition thread
	startSpeechRecognition();
    }

    /**
     * Starts the Speech Recognition Thread
     */
    public synchronized static void startSpeechRecognition() {

	// Check lock
	if (!speechRecognizerThreadRunning)
	    // logger.log(Level.INFO, "Speech Recognition Thread already
	    // running...\n");

	    // Submit to ExecutorService
	    eventsExecutorService.submit(() -> {

		// locks
		speechRecognizerThreadRunning = true;
		ignoreSpeechRecognitionResults = false;

		while (speechRecognizerThreadRunning) {
		   // System.out.print("test");
		    synchronized(syncObject) {
			    try {
			        // Calling wait() will block this thread until another thread
			        // calls notify() on the object.
			        syncObject.wait();
			    } catch (InterruptedException e) {
			        // Happens if someone interrupts your thread.
			    }
			}
		    changeLanguage(nlpAgentLanguage);

		    // Start Recognition
		    recognizer.startRecognition(true);

		    // Information

		    System.out.println("You can start to speak...");

		    try {
			SpeechResult speechResult;
			while (speechRecognizerThreadRunning) {
			    // System.out.println("Infinite looping");
			    if (changedLanguage) {
				// flag = true;
				// speechRecognizerThreadRunning = false;
				break;
			    }

			    speechResult = recognizer.getResult();
			    
//			    System.out.println( speechResult.getResult());
//			    List<WordResult> wordResults = speechResult.getWords();
//			    for (WordResult word:wordResults)
//			    {
//				System.out.println(word.getPronunciation());
//				//System.out.println(word.getConfidence());
//				System.out.println(word.toString());
//				//System.out.println(word.getWord());
//			    }
			    // Check if we ignore the speech recognition
			    // results
			    if (!ignoreSpeechRecognitionResults) {

				// Check the result
				if (speechResult != null) {

				    // Get the hypothesis
				    speechRecognitionResult = speechResult.getHypothesis();
				    String result = speechRecognitionResult.toLowerCase();

				    if ((result.equals("aloha bot")) || (result.equals("aloha pana"))) {
					nlpAgentState = RUNNING;
				    } else {
					if (nlpAgentLanguage.equals(ENGLISH))
					{
					    // TODO nothing
					}
				        else if (nlpAgentLanguage.equals(VIETNAMESE)) {
					    result = compound2Unicode(result);
					} else if (nlpAgentLanguage.equals(JAPANESE)) {
					    if ((!result.equals("aloha bot")) && (!result.equals("aloha pana"))) {
						result = "<unk>";
					    }
					}
				    }

//				    String scorestr = String.valueOf(speechResult.getResult().getBestFinalToken().getScore());
				    // You said?
				    
//				    System.out.println("Score:" + scorestr);

				    if (nlpAgentState.equals(BUSY)) {
//					System.out.print("BUSY");
					if (result.equalsIgnoreCase("aloha bot") || result.equalsIgnoreCase("aloha pana")) {
					    out.println(result);
					}
				    } else if (nlpAgentState.equals(RUNNING)) {
//					System.out.print("RUNNING");
					out.println(result);
				    }
				    System.out.println("You said: [" + result + "]\n");
				    speechResult = null;
				}
			    } else
				// logger.log(Level.INFO, "");
				System.out.println("Ignoring Speech Recognition Results...");

			}
		    } catch (Exception ex) {
			// logger.log(Level.WARNING, null, ex);
			speechRecognizerThreadRunning = false;
		    }
		    System.out.println("Change language");
		}
		recognizer.stopRecognition();
	    });
    }

    public synchronized static void clientSocket() throws IOException {
	// Check lock
	if (!clientThreadRunning)
	    // logger.log(Level.INFO, "Resources Thread already running...\n");

	    // Submit to ExecutorService
	    eventsExecutorService.submit(() -> {
		try {

		    // Lock
		    clientThreadRunning = true;

		    // Detect if the microphone is available
		    while (true) {
			// get signal from NLPAgent

			inputline = in.readLine();
			System.out.println("Message received: " + inputline);
			// if inputline has some text, change NLPinfo called
			if (inputline.length() > 0 ) {
			    if (inputline.contains(INITSYS))
			    {
				nlpAgentLanguage = inputline.replace(INITSYS, "");
				//synchronized INIT = true;
				synchronized(syncObject) {
				    syncObject.notify();
				}
			    }
			    else
			    {
				 // if nlpAgentMode changed, go out from this
				    if (changeNlpInfo(inputline)) {
					changedLanguage = true;
				    }

			    }   
			}
		    }

		} catch (Exception ex) {
		    // logger.log(Level.WARNING, null, ex);
		    clientThreadRunning = false;

		}
	    });
    }

    private static boolean changeNlpInfo(String input) {
	boolean ret = false;
	switch (input) {
	case VIETNAMESE:
	    if (!nlpAgentLanguage.equals(input))
		nlpAgentLanguage = input;
	    ret = true;
	    break;
	case JAPANESE:
	    if (!nlpAgentLanguage.equals(input))
		nlpAgentLanguage = input;
	    ret = true;
	    break;
	case ENGLISH:
	    if (!nlpAgentLanguage.equals(input))
		nlpAgentLanguage = input;
	    ret = true;
	    break;
	case RUNNING:
	    nlpAgentState = input;
	    break;
	case BUSY:
	    nlpAgentState = input;
	    break;
	}

	return ret;
    }

    public static void changeLanguage(String language_code) {
	switch (language_code) {
	case ENGLISH:
	    // recognizer = recognizerEng;
	    try {
		configEnglish();
		recognizer = new LiveSpeechRecognizer(configuration);
	    } catch (IOException ex) {
		// logger.log(Level.SEVERE, null, ex);
		ex.printStackTrace();
	    }
	    break;
	case VIETNAMESE:
	    // recognizer = recognizerViet;
	    try {
		configVietnamese();
		recognizer = new LiveSpeechRecognizer(configuration);
	    } catch (IOException ex) {
		// logger.log(Level.SEVERE, null, ex);
		ex.printStackTrace();
	    }
	    break;
	case JAPANESE:
	    try {
		configEnglish();
		recognizer = new LiveSpeechRecognizer(configuration);
	    } catch (IOException ex) {
		// logger.log(Level.SEVERE, null, ex);
		ex.printStackTrace();
	    }
	    break;
	default:
	    System.out.println("Unknown language");
	}
	changedLanguage = false;
    }

    // -----------------------------------------------------------------------------------------------
    public static void configVietnamese() {
	// Load model from the jar
	// configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
	configuration.setAcousticModelPath("resource:modelname");
	// configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
	configuration.setDictionaryPath("resource:modelname.dic");

	// ====================================================================================
	// =====================READ
	// THIS!!!===============================================
	// Uncomment this line of code if you want the recognizer to recognize
	// every word of the language
	// you are using , here it is English for example
	// ====================================================================================
	// configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");
	configuration.setLanguageModelPath("resource:modelname.bin");
	//
	// ====================================================================================
	// =====================READ
	// THIS!!!===============================================
	// If you don't want to use a grammar file comment below 3 lines and
	// uncomment the above line for language model
	// ====================================================================================

	// Grammar
	configuration.setGrammarPath("resource:/grammars");
	configuration.setGrammarName("grammar_vi");
	// configuration.setGrammarName("grammar");
	// configuration.setGrammarName("grammar_old");
	configuration.setUseGrammar(true);
    }

    public static void configEnglish() {
	// Load model from the jar
	// configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
	configuration.setAcousticModelPath("resource:/models/en-us-adapt");
	// configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
	configuration.setDictionaryPath("resource:/models/1767.dic");

	// ====================================================================================
	// =====================READ
	// THIS!!!===============================================
	// Uncomment this line of code if you want the recognizer to recognize
	// every word of the language
	// you are using , here it is English for example
	// ====================================================================================
	// configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");
	configuration.setLanguageModelPath("resource:/models/1767.lm.bin");
	//
	// ====================================================================================
	// =====================READ
	// THIS!!!===============================================
	// If you don't want to use a grammar file comment below 3 lines and
	// uncomment the above line for language model
	// ====================================================================================

	// Grammar
	configuration.setGrammarPath("resource:/grammars");
	configuration.setGrammarName("grammar_demo");
	// configuration.setGrammarName("grammar");
	// configuration.setGrammarName("grammar_old");
	configuration.setUseGrammar(true);
    }

    public static void configJapanese() {
	// TODO: Setting configure for Japanese
	System.out.println("configure Japanese");

    }

    /**
     * Stops ignoring the results of SpeechRecognition
     */
    public synchronized void stopIgnoreSpeechRecognitionResults() {

	// Stop ignoring speech recognition results
	ignoreSpeechRecognitionResults = false;
    }

    /**
     * Ignores the results of SpeechRecognition
     */
    public synchronized void ignoreSpeechRecognitionResults() {

	// Instead of stopping the speech recognition we are ignoring it's
	// results
	ignoreSpeechRecognitionResults = true;

    }

    // -----------------------------------------------------------------------------------------------

    /**
     * Starting a Thread that checks if the resources needed to the
     * SpeechRecognition library are available
     */
    public static void startResourcesThread() {

	// Check lock
	if (!resourcesThreadRunning)
	    // logger.log(Level.INFO, "Resources Thread already running...\n");

	    // Submit to ExecutorService
	    eventsExecutorService.submit(() -> {
		try {

		    // Lock
		    resourcesThreadRunning = true;

		    // Detect if the microphone is available
		    while (true) {

			// Is the Microphone Available
			if (!AudioSystem.isLineSupported(Port.Info.MICROPHONE))
			    // logger.log(Level.INFO, "Microphone is not
			    // available.\n");
			    // System.out.println("Microphone is not
			    // available");

			    // Sleep some period
			    Thread.sleep(350);
		    }

		} catch (InterruptedException ex) {
		    // logger.log(Level.WARNING, null, ex);
		    resourcesThreadRunning = false;

		}
	    });
    }

    /**
     * Takes a decision based on the given result
     * 
     * @param speechWords
     */
    public void makeDecision(String speech, List<WordResult> speechWords) {
	System.out.println(speech);

    }

    public boolean getIgnoreSpeechRecognitionResults() {
	return ignoreSpeechRecognitionResults;
    }

    public boolean getSpeechRecognizerThreadRunning() {
	return speechRecognizerThreadRunning;
    }

    public static String compound2Unicode(String str) {
	System.out.println("raw: " + str);
	str = str.replaceAll("\u0065\u0065\u0072", "\u1EC3"); // ể
	str = str.replaceAll("\u0065\u0065\u0073", "\u1EBF"); // ế
	str = str.replaceAll("\u0065\u0065\u0066", "\u1EC1"); // ề
	str = str.replaceAll("\u0065\u0065\u006A", "\u1EC7"); // ệ
	str = str.replaceAll("\u0065\u0065\u0078", "\u1EC5"); // ễ
	str = str.replaceAll("\u0065\u0065", "\u00EA"); // ee - ê

	str = str.replaceAll("\u0065\u0072", "\u1EBB"); // er - ẻ
	str = str.replaceAll("\u0065\u0073", "\u00E9"); // es - é
	str = str.replaceAll("\u0065\u0066", "\u00E8"); // ef - è
	str = str.replaceAll("\u0065\u006A", "\u1EB9"); // ej - ẹ
	str = str.replaceAll("\u0065\u0078", "\u1EBD"); // ex - ẽ

	str = str.replaceAll("\u0079\u0072", "\u1EF7"); // ỷ
	str = str.replaceAll("\u0079\u0073", "\u00FD"); // ý
	str = str.replaceAll("\u0079\u0066", "\u1EF3"); // ỳ
	str = str.replaceAll("\u0079\u006A", "\u1EF5"); // ỵ
	str = str.replaceAll("\u0079\u0078", "\u1EF9"); // ỹ

	str = str.replaceAll("\u0075\u0077\u0072", "\u1EED"); // ử
	str = str.replaceAll("\u0075\u0077\u0073", "\u1EE9"); // ứ
	str = str.replaceAll("\u0075\u0077\u0066", "\u1EEB"); // ừ
	str = str.replaceAll("\u0075\u0077\u006A", "\u1EF1"); // ự
	str = str.replaceAll("\u0075\u0077\u0078", "\u1EEF"); // ữ
	str = str.replaceAll("\u0075\u0077", "\u01B0"); // ư

	str = str.replaceAll("\u0075\u0072", "\u1EE7"); // ủ
	str = str.replaceAll("\u0075\u0073", "\u00FA"); // ú
	str = str.replaceAll("\u0075\u0066", "\u00F9"); // ù
	str = str.replaceAll("\u0075\u006A", "\u1EE5"); // ụ
	str = str.replaceAll("\u0075\u0078", "\u0169"); // ũ

	str = str.replaceAll("\u0069\u0072", "\u1EC9"); // ỉ
	str = str.replaceAll("\u0069\u0073", "\u00ED"); // í
	str = str.replaceAll("\u0069\u0066", "\u00EC"); // ì
	str = str.replaceAll("\u0069\u006A", "\u1ECB"); // ị
	str = str.replaceAll("\u0069\u0078", "\u0129"); // ĩ

	str = str.replaceAll("\u006F\u0077\u0072", "\u1EDF"); // ở
	str = str.replaceAll("\u006F\u0077\u0073", "\u1EDB"); // ớ
	str = str.replaceAll("\u006F\u0077\u0066", "\u1EDD"); // ờ
	str = str.replaceAll("\u006F\u0077\u006A", "\u1EE3"); // ợ
	str = str.replaceAll("\u006F\u0077\u0078", "\u1EE1"); // ỡ
	str = str.replaceAll("\u006F\u0077", "\u01A1"); // ơ

	str = str.replaceAll("\u006F\u006F\u0072", "\u1ED5"); // ổ
	str = str.replaceAll("\u006F\u006F\u0073", "\u1ED1"); // ố
	str = str.replaceAll("\u006F\u006F\u0066", "\u1ED3"); // ồ
	str = str.replaceAll("\u006F\u006F\u006A", "\u1ED9"); // ộ
	str = str.replaceAll("\u006F\u006F\u0078", "\u1ED7"); // ỗ
	str = str.replaceAll("\u006F\u006F", "\u00F4"); // ô

	str = str.replaceAll("\u006F\u0072", "\u1ECF"); // ỏ
	str = str.replaceAll("\u006F\u0073", "\u00F3"); // ó
	str = str.replaceAll("\u006F\u0066", "\u00F2"); // ò
	str = str.replaceAll("\u006F\u006A", "\u1ECD"); // ọ
	str = str.replaceAll("\u006F\u0078", "\u00F5"); // õ

	str = str.replaceAll("\u0061\u0077\u0072", "\u1EB3"); // ẳ
	str = str.replaceAll("\u0061\u0077\u0073", "\u1EAF"); // ắ
	str = str.replaceAll("\u0061\u0077\u0066", "\u1EB1"); // ằ
	str = str.replaceAll("\u0061\u0077\u006A", "\u1EB7"); // ặ
	str = str.replaceAll("\u0061\u0077\u0078", "\u1EB5"); // ẵ
	str = str.replaceAll("\u0061\u0077", "\u0103"); // ă

	str = str.replaceAll("\u0061\u0061\u0072", "\u1EA9"); // ẩ
	str = str.replaceAll("\u0061\u0061\u0073", "\u1EA5"); // ấ
	str = str.replaceAll("\u0061\u0061\u0066", "\u1EA7"); // ầ
	str = str.replaceAll("\u0061\u0061\u006A", "\u1EAD"); // ậ
	str = str.replaceAll("\u0061\u0061\u0078", "\u1EAB"); // ẫ
	str = str.replaceAll("\u0061\u0061", "\u00E2"); // â

	str = str.replaceAll("\u0061\u0072", "\u1EA3"); // ả
	str = str.replaceAll("\u0061\u0073", "\u00E1"); // á
	str = str.replaceAll("\u0061\u0066", "\u00E0"); // à
	str = str.replaceAll("\u0061\u006A", "\u1EA1"); // ạ
	str = str.replaceAll("\u0061\u0078", "\u00E3"); // ã

	str = str.replaceAll("\u0064\u0064", "\u0111"); // đ

	return str;
    }

    /**
     * Main Method
     * 
     * @param args
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static void main(String[] args) {
	String serverAddress = "127.0.0.1";
	try {
	    Socket socket = new Socket(serverAddress, 9090);
	    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	    out = new PrintWriter(socket.getOutputStream(), true);
	    new SpeechRecognizerMain();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}