package ch.ntb.inf.deep.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import ch.ntb.inf.deep.debug.Dbg;
import ch.ntb.inf.deep.host.ErrorReporter;
import ch.ntb.inf.deep.strings.HString;

public class Parser implements ErrorCodes, IAttributes {

	// private static final boolean DEBUG = true;

	// -------- Invalide Symbol:
	private static final short g0 = 0, sUndef = g0;
	// -------- Bracket: "(", ")", "{", "}"
	private static final short g1 = g0 + 1, sLParen = g1, sRParen = g1 + 1,
			sLBrace = g1 + 2, sRBrace = g1 + 3, sLBracket = g1 + 4,
			sRBracket = g1 + 5;
	// -------- Punctuation mark ",", '"', ";", "."
	private static final short g2 = g1 + 6, sComma = g2,
			sQuotationMark = g2 + 1, sSemicolon = g2 + 2, sDot = g2 + 3,
			sColon = g2 + 4;
	// -------- Math op.: "*", "/", "+", "-"
	private static final short g3 = g2 + 5, sMul = g3, sDiv = g3 + 1,
			sPlus = g3 + 2, sMinus = g3 + 3;
	// -------- Assignment op.: "="
	private static final short g4 = g3 + 4, sEqualsSign = g4, sAt = g4 + 1;
	// -------- Access attribute : "read", "write"
	private static final short g5 = g4 + 2, sRead = g5, sWrite = g5 + 1;
	// -------- Content attribute: "const", "code", "var", "heap", "stack",
	// "systab"
	private static final short g6 = g5 + 2, sConst = g6, sCode = g6 + 1,
			sVar = g6 + 2, sHeap = g6 + 3, sStack = g6 + 4, sSysTab = g6 + 5,
			sDefault = g6 + 6;
	// -------- Register type: "GPR", "FPR", "SPR"
	public static final short g7 = g6 + 7, sGPR = g7, sFPR = g7 + 1,
			sSPR = g7 + 2;
	// -------- Register representation: "HEX", "DEZ", "BIN", "FLOAT"
	public static final short g8 = g7 + 3, sHex = g8, sDez = g8 + 1,
			sBin = g8 + 2, sFloat = g8 + 3;
	// -------- Assignment keywords; "version", "description", "import",
	// "device", "attributes", "width", "size", "base",
	// "programmer", "rootclasses", "segmentsize", "arraysize", "nofsegments",
	// "xx", "xxx"
	// "kernel", "heap", "interrupt", "exception", "addr", "type", "repr",
	// "libpath", "debuglevel", "printlevel"
	private static final short g9 = g8 + 4, sVersion = g9,
			sDescription = g9 + 1, sImport = g9 + 2, sAttributes = g9 + 3,
			sWidth = g9 + 4, sSize = g9 + 5, sBase = g9 + 6,
			sRootclasses = g9 + 7, sSegmentsize = g9 + 8, sArraysize = g9 + 9,
			sNofsegements = g9 + 10, sKernel = g9 + 11, sInterrupt = g9 + 12,
			sException = g9 + 13, sXx = g9 + 14, sXxx = g9 + 15,
			sAddr = g9 + 16, sType = g9 + 17, sRepr = g9 + 18,
			sLibPath = g9 + 19, sDebugLevel = g9 + 20, sPrintLevel = g9 + 21;
	// -------- Block keywords: "meta", "constants", " device", "reginit",
	// "segment", "memorymap", "map", "modules", "project",
	// "segmentarray", register", operatingsystem, sysconst
	private static final short g10 = g9 + 22, sMeta = g10,
			sConstants = g10 + 1, sDevice = g10 + 2, sReginit = g10 + 3,
			sSegment = g10 + 4, sMemorymap = g10 + 5, sMap = g10 + 6,
			sModules = g10 + 7, sTargetConf = g10 + 8, sProject = g10 + 9,
			sSegmentarray = g10 + 10, sRegistermap = g10 + 11,
			sRegister = g10 + 12, sOperatingSystem = g10 + 13,
			sSysConst = g10 + 14;
	// -------- Designator, IntNumber,
	private static final short g11 = g10 + 15, sDesignator = g11,
			sNumber = g11 + 1;
	// -------- End of file: EOF
	private static final short g12 = g11 + 2, sEndOfFile = g12;

	private static int nOfErrors;
	private static int sym;
	private static String strBuffer;
	private static int chBuffer;
	private static int intNumber;
	private static ErrorReporter reporter = ErrorReporter.reporter;
	private static ArrayList<String> importedFiles = new ArrayList<String>();
	private static String loc;
	private static String libPath;

	private BufferedReader configFile;
	private ArrayList<String> importList;

	public static void main(String[] args) {
		parseAndCreateConfig("D:/work/Crosssystem/deep", "rsc/MyProject.deep");
		Dbg.vrb.println("Config read with " + nOfErrors + " error(s)");
		Configuration.print();
		Configuration.createInterfaceFile("D:/work/Crosssystem/deep", "rsc/TestInterface.java");
	}

	public Parser(String file) {
		try {
			importList = new ArrayList<String>();
			configFile = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			reporter.error(errIOExp, e.getMessage());
		}
	}

	public static void parseAndCreateConfig(String location, String file) {
		String fileToRead;
		importedFiles.add(file);
		if (Parser.loc == null) {// save only the first location by the first
			// call of the static method
			// parseAndCreateConfig(...)
			if (location.endsWith("/")) {
				fileToRead = location + file;
				Parser.loc = location;
			} else {
				fileToRead = location + "/" + file;
				Parser.loc = location + "/";
			}
		} else if (location.endsWith("/")) {
			fileToRead = location + file;
		} else {
			fileToRead = location + "/" + file;
		}
		Parser par = new Parser(fileToRead);
		par.config();
	}

	private int config() {
		// read first Symbol
		next();

		meta();

		while (sym != sEndOfFile) {
			switch (sym) {
			case sConstants:
				constants();
				break;
			case sMemorymap:
				memoryMap();
				break;
			case sRegistermap:
				registermap();
				break;
			case sTargetConf:
				targetconfiguration();
				break;
			case sReginit:
				regInit();
				break;
			case sProject:
				project();
				break;
			case sOperatingSystem:
				operatingSystem();
				break;
			case sSysConst:
				sysconst();
				break;
			default:
				nOfErrors++;
				reporter.error(errUnexpectetSymExp,
								"expectet symbol : constants | sysconst | memorymap | registermap | targetconfiguration | reginit | project | operatingsystem, received symbol: "
										+ symToString() + "\n");
				next();
			}
		}
		return 0;
	}

	/**
	 * Reads a String from the Configfile
	 * 
	 * @return a String
	 */
	private String readString() {
		if (sym != sQuotationMark) {
			nOfErrors++;
			reporter.error(errQuotationMarkExp,
					"expected symbol: \", received symbol: " + symToString()
							+ " ");
			return "";
		}
		StringBuffer sb = new StringBuffer();
		int ch;
		try {
			ch = configFile.read();
			while ((ch > 34 && ch <= 126) || ch == ' ' || ch == '!') {
				sb.append((char) ch);
				ch = configFile.read();
			}
			chBuffer = ch;
			next();
			if (sym != sQuotationMark) {
				nOfErrors++;
				reporter.error(errQuotationMarkExp,
						"expected symbol: \", received symbol: "
								+ symToString() + " ");
				return "";
			}
		} catch (IOException e) {
			reporter.error(errIOExp, e.getMessage());
		}
		next();
		return sb.toString();
	}

	private static boolean isKeyword(String str) {
		String temp = str.toLowerCase();// only for keywords for which the case
		// sensitivity will not be considered
		sym = sDesignator;
		switch (temp.charAt(0)) {
		case 'a':
			if (str.equals("arraysize")) {
				sym = sArraysize;
				return true;
			} else if (str.equals("attributes")) {
				sym = sAttributes;
				return true;
			} else if (str.equals("addr")) {
				sym = sAddr;
				return true;
			}
			break;
		case 'b':
			if (str.equals("base")) {
				sym = sBase;
				return true;
			} else if (temp.equals("bin")) {
				sym = sBin;
				return true;
			}
			break;
		case 'c':
			if (str.equals("code")) {
				sym = sCode;
				return true;
			} else if (str.equals("const")) {
				sym = sConst;
				return true;
			} else if (str.equals("constants")) {
				sym = sConstants;
				return true;
			}
			break;
		case 'd':
			if (temp.equals("dez")) {
				sym = sDez;
				return true;
			} else if (str.equals("device")) {
				sym = sDevice;
				return true;
			} else if (str.equals("description")) {
				sym = sDescription;
				return true;
			} else if (str.equals("debuglevel")) {
				sym = sDebugLevel;
				return true;
			} else if (str.equals("default")) {
				sym = sDefault;
				return true;
			}
			break;
		case 'e':
			if (str.equals("exception")) {
				sym = sException;
				return true;
			}
			break;
		case 'f':
			if (temp.equals("float")) {
				sym = sFloat;
				return true;
			} else if (temp.equals("fpr")) {
				sym = sFPR;
				return true;
			}
			break;
		case 'g':
			if (temp.equals("gpr")) {
				sym = sGPR;
				return true;
			}
			break;
		case 'h':
			if (str.equals("heap")) {
				sym = sHeap;
				return true;
			} else if (temp.equals("hex")) {
				sym = sHex;
				return true;
			}
			break;
		case 'i':
			if (str.equals("import")) {
				sym = sImport;
				return true;
			} else if (str.equals("interrupt")) {
				sym = sInterrupt;
				return true;
			}
			break;
		case 'k':
			if (str.equals("kernel")) {
				sym = sKernel;
				return true;
			}
			break;
		case 'l':
			if (str.equals("libpath")) {
				sym = sLibPath;
				return true;
			}
		case 'm':
			if (str.equals("map")) {
				sym = sMap;
				return true;
			} else if (str.equals("modules")) {
				sym = sModules;
				return true;
			} else if (str.equals("meta")) {
				sym = sMeta;
				return true;
			} else if (str.equals("memorymap")) {
				sym = sMemorymap;
				return true;
			}
			break;
		case 'n':
			if (str.equals("nofsegments")) {
				sym = sNofsegements;
				return true;
			}
			break;
		case 'o':
			if (str.equals("operatingsystem")) {
				sym = sOperatingSystem;
				return true;
			}
		case 'p':
			if (str.equals("project")) {
				sym = sProject;
				return true;
			} else if (str.equals("printlevel")) {
				sym = sPrintLevel;
				return true;
			}
			break;
		case 'r':
			if (str.equals("read")) {
				sym = sRead;
				return true;
			} else if (str.equals("register")) {
				sym = sRegister;
				return true;
			} else if (str.equals("repr")) {
				sym = sRepr;
				return true;
			} else if (str.equals("registermap")) {
				sym = sRegistermap;
				return true;
			} else if (str.equals("reginit")) {
				sym = sReginit;
				return true;
			} else if (str.equals("rootclasses")) {
				sym = sRootclasses;
				return true;
			}
			break;
		case 's':
			if (str.equals("size")) {
				sym = sSize;
				return true;
			} else if (str.equals("stack")) {
				sym = sStack;
				return true;
			} else if (str.equals("segment")) {
				sym = sSegment;
				return true;
			} else if (str.equals("systab")) {
				sym = sSysTab;
				return true;
			} else if (str.equals("segmentsize")) {
				sym = sSegmentsize;
				return true;
			} else if (str.equals("segmentarray")) {
				sym = sSegmentarray;
				return true;
			} else if (temp.equals("spr")) {
				sym = sSPR;
				return true;
			} else if (str.equals("sysconst")) {
				sym = sSysConst;
				return true;
			}
			break;
		case 't':
			if (str.equals("type")) {
				sym = sType;
				return true;
			} else if (str.equals("targetconfiguration")) {
				sym = sTargetConf;
				return true;
			}
		case 'v':
			if (str.equals("var")) {
				sym = sVar;
				return true;
			} else if (str.equals("version")) {
				sym = sVersion;
				return true;
			}
			break;
		case 'w':
			if (str.equals("width")) {
				sym = sWidth;
				return true;
			} else if (str.equals("write")) {
				sym = sWrite;
				return true;
			}
			break;
		case 'x':
			if (str.equals("xx")) {
				sym = sXx;
				return true;
			} else if (str.equals("xxx")) {
				sym = sXxx;
				return true;
			}
		default:
			return false;
		}
		return false;
	}

	/**
	 * determine the next symbol ignores tabs and spaces
	 */
	private void next() {
		int ch = 0;
		try {
			if (chBuffer != 0) {
				ch = chBuffer;
				chBuffer = 0;
			} else {
				ch = configFile.read();
			}
			switch (ch) {
			case '#':
				configFile.readLine();
				next();
				break; // Ignore comments
			case '\t':
			case ' ':
			case '\r':
			case '\n':
				next();
				break; // Ignore spaces, tabs and CR
			case '{':
				sym = sLBrace;
				break;
			case '}':
				sym = sRBrace;
				break;
			case '(':
				sym = sLParen;
				break;
			case ')':
				sym = sRParen;
				break;
			case '[':
				sym = sLBracket;
				break;
			case ']':
				sym = sRBracket;
				break;
			case '*':
				sym = sMul;
				break;
			case '/':
				sym = sDiv;
				break;
			case '+':
				sym = sPlus;
				break;
			case '-':
				sym = sMinus;
				break;
			case ',':
				sym = sComma;
				break;
			case '"':
				sym = sQuotationMark;
				break;
			case '=':
				sym = sEqualsSign;
				break;
			case ';':
				sym = sSemicolon;
				break;
			case '@':
				sym = sAt;
				break;
			case '.':
				sym = sDot;
				break;
			case ':':
				sym = sColon;
				break;
			default:
				String s;
				StringBuffer sb;
				if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')
						|| ch == '_') {// Designator or Keyword
					sb = new StringBuffer();
					do {
						sb.append((char) ch);
						ch = configFile.read();
					} while ((ch >= 'a' && ch <= 'z')
							|| (ch >= 'A' && ch <= 'Z')
							|| (ch >= '0' && ch <= '9') || ch == '_');
					chBuffer = ch;
					s = sb.toString();
					if (!isKeyword(s)) {
						strBuffer = s;
					}
				} else if (ch >= '0' && ch <= '9') { // Number
					sym = sNumber;
					intNumber = 0;
					if (ch == '0') {
						ch = configFile.read();
						if (ch == 'x' || ch == 'X') {// its maybe a hex digit
							sb = new StringBuffer();
							sb.append("0x");
							ch = configFile.read();
							while ((ch >= '0' && ch <= '9')
									|| (ch >= 'a' && ch <= 'f')
									|| (ch >= 'A' && ch <= 'F')) {
								sb.append((char) ch);
								ch = configFile.read();
							}
							chBuffer = ch;
							intNumber = Integer.decode(sb.toString());
							break;
						} else if (ch == ';') {
							chBuffer = ch;
							return;
						} else {// check if it is a digit
							if (!(ch >= '0' && ch <= '9')) {
								nOfErrors++;
								reporter.error(errDigitExp, "Invalide Number");
								chBuffer = ch;
								break;
							}
						}
					}
					do {
						intNumber = intNumber * 10 + ch - '0';
						ch = configFile.read();
					} while (ch >= '0' && ch <= '9');
					chBuffer = ch;
				} else if (ch == -1)
					sym = sEndOfFile;
				else
					sym = sUndef;
			}
		} catch (IOException e) {
			reporter.error(errIOExp, e.getMessage());
		}
	}

	private static String symToString() {
		return symToString(sym);
	}

	private static String symToString(int sym) {
		switch (sym) {
		case sUndef:
			return "undefine";
		case sLParen:
			return "(";
		case sRParen:
			return ")";
		case sLBrace:
			return "{";
		case sRBrace:
			return "}";
		case sLBracket:
			return "[";
		case sRBracket:
			return "]";
		case sComma:
			return ",";
		case sQuotationMark:
			return "\"";
		case sSemicolon:
			return ";";
		case sDot:
			return ".";
		case sColon:
			return ":";
		case sMul:
			return "*";
		case sDiv:
			return "/";
		case sPlus:
			return "+";
		case sMinus:
			return "-";
		case sEqualsSign:
			return "=";
		case sAt:
			return "@";
		case sRead:
			return "read";
		case sWrite:
			return "write";
		case sConst:
			return "const";
		case sCode:
			return "code";
		case sVar:
			return "var";
		case sHeap:
			return "heap";
		case sStack:
			return "stack";
		case sSysTab:
			return "systab";
		case sDefault:
			return "default";
		case sGPR:
			return "gpr";
		case sFPR:
			return "fpr";
		case sSPR:
			return "spr";
		case sHex:
			return "hex";
		case sDez:
			return "dez";
		case sBin:
			return "bin";
		case sFloat:
			return "float";
		case sVersion:
			return "version";
		case sDescription:
			return "description";
		case sImport:
			return "import";
		case sAttributes:
			return "attributes";
		case sWidth:
			return "width";
		case sSize:
			return "size";
		case sBase:
			return "base";
		case sSysConst:
			return "sysconst";
		case sRootclasses:
			return "rootclasses";
		case sSegmentsize:
			return "segmentsize";
		case sArraysize:
			return "arraysize";
		case sNofsegements:
			return "nofsegements";
		case sKernel:
			return "kernel";
		case sInterrupt:
			return "interrupt";
		case sException:
			return "exception";
		case sXx:
			return "xx";
		case sXxx:
			return "xxx";
		case sAddr:
			return "addr";
		case sType:
			return "type";
		case sRepr:
			return "repr";
		case sLibPath:
			return "libpath";
		case sDebugLevel:
			return "debuglevel";
		case sPrintLevel:
			return "printlevel";
		case sMeta:
			return "meta";
		case sConstants:
			return "constants";
		case sDevice:
			return "device";
		case sReginit:
			return "reginit";
		case sSegment:
			return "segment";
		case sMemorymap:
			return "memorymap";
		case sMap:
			return "map";
		case sModules:
			return "modules";
		case sTargetConf:
			return "targetconfiguration";
		case sProject:
			return "project";
		case sSegmentarray:
			return "segmentarray";
		case sRegistermap:
			return "registermap";
		case sRegister:
			return "register";
		case sOperatingSystem:
			return "operatingsystem";
		case sDesignator:
			return "designator";
		case sNumber:
			return "number";
		case sEndOfFile:
			return "endoffile";
		default:
			return "";
		}
	}

	private void meta() {
		if (sym != sMeta) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: meta, received symbol: " + symToString()
							+ " ");
			return;
		}
		next();
		if (sym != sLBrace) {
			nOfErrors++;
			reporter.error(errLBraceExp,
					"expected symbol: {, received symbol: " + symToString()
							+ " ");
			return;
		}
		next();
		versionAssignment();
		fileDescAssignment();
		if (sym == sImport) {
			importAssignment();
		}
		if (sym != sRBrace) {
			nOfErrors++;
			reporter.error(errRBraceExp,
					"expected symbol: }, received symbol: " + symToString()
							+ " ");
			return;
		}
		if (Configuration.getProject() != null) {
			for (int i = 0; i < importList.size(); i++) {
				String toCmp = importList.get(i);
				if (!importedFiles.contains(toCmp)) {
					importedFiles.add(toCmp);
					File f = new File(loc + toCmp);
					if (f.exists()) {
						parseAndCreateConfig(loc, toCmp);
					} else {
						parseAndCreateConfig(libPath, toCmp);
					}
				}
			}
			// delete importList to prevent to do imports twice(in "project()")
			importList = null;
		}
		next();
	}

	private void constants() {
		if (sym != sConstants) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: constants, received symbol: "
							+ symToString() + " ");
			return;
		}
		next();
		if (sym != sLBrace) {
			nOfErrors++;
			reporter.error(errLBraceExp,
					"expected symbol: {, received symbol: " + symToString()
							+ " ");
			return;
		}
		next();
		Consts constants = Consts.getInstance();
		while (sym == sDesignator) {
			constants.addConst(HString.getHString(strBuffer), varAssignment());
		}
		if (sym != sRBrace) {
			nOfErrors++;
			reporter.error(errRBraceExp,
					"expected symbol: }, received symbol: " + symToString()
							+ " ");
			return;
		}
		next();
	}

	private void sysconst() {
		if (sym != sSysConst) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: sysconst, received symbol: "
							+ symToString() + " ");
			return;
		}
		next();
		if (sym != sLBrace) {
			nOfErrors++;
			reporter.error(errLBraceExp,
					"expected symbol: {, received symbol: " + symToString()
							+ " ");
			return;
		}
		next();
		SystemConstants sysConst = SystemConstants.getInstance();
		while (sym == sDesignator) {
			sysConst.addSysConst(HString.getHString(strBuffer), varAssignment());
		}
		if (sym != sRBrace) {
			nOfErrors++;
			reporter.error(errRBraceExp,
					"expected symbol: }, received symbol: " + symToString()
							+ " ");
			return;
		}
		next();
	}

	private void memoryMap() {
		if (sym != sMemorymap) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: memorymap, received symbol: "
							+ symToString() + " ");
			return;
		}
		next();
		if (sym != sLBrace) {
			nOfErrors++;
			reporter.error(errLBraceExp,
					"expected symbol: {, received symbol: " + symToString()
							+ " ");
			return;
		}
		next();
		MemoryMap memMap = MemoryMap.getInstance();
		while (sym == sDevice || sym == sSegment || sym == sReginit
				|| sym == sModules || sym == sSegmentarray) {
			if (sym == sDevice) {
				memMap.addDevice(device());
			} else if (sym == sSegment) {
				memMap.addSegment(segment(false, 0, 0));
			} else if (sym == sReginit) {
				regInit();
			} else if (sym == sSegmentarray) {
				segmentArray(false, null);
			} else {// sModules
				modules(null);
			}
		}
		if (sym != sRBrace) {
			nOfErrors++;
			reporter.error(errRBraceExp,
					"expected symbol: }, received symbol: " + symToString()
							+ " ");
			return;
		}
		next();
	}

	private Device device() {
		if (sym != sDevice) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: device, received symbol: "
							+ symToString() + " ");
			return null;
		}
		next();
		if (sym != sDesignator) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: designator, received symbol: "
							+ symToString() + " ");
			return null;
		}
		HString dev = HString.getHString(strBuffer);
		next();
		if (sym != sLBrace) {
			nOfErrors++;
			reporter.error(errLBraceExp,
					"expected symbol: {, received symbol: " + symToString()
							+ " ");
			return null;
		}
		next();
		int attributes = 0;
		int base = 0;
		int width = 0;
		int size = 0;
		while (sym == sAttributes || sym == sBase || sym == sWidth
				|| sym == sSize) {
			if (sym == sAttributes) {
				attributes = attributeAssignment();
			} else if (sym == sBase) {
				base = baseAssignment();
			} else if (sym == sWidth) {
				width = widthAssignment();
			} else {// sSize
				size = sizeAssignment();
			}
		}
		if (sym != sRBrace) {
			nOfErrors++;
			reporter.error(errRBraceExp,
					"expected symbol: }, received symbol: " + symToString()
							+ " ");

			return null;
		}
		if (attributes == 0 || width == 0 || size == 0) {
			reporter.error(errInconsistentattributes,
					"Missing attribute by creation of device: "
							+ dev.toString() + "\n");
			return null;
		}
		next();
		return new Device(dev, base, size, width, attributes);
	}

	private Segment segment(boolean isSubSegment, int inheritAttributes,
			int inheritWidth) {
		if (sym != sSegment) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: segment, received symbol: "
							+ symToString() + " ");
			return null;
		}
		next();
		Segment seg = new Segment(segmentDesignator());
		// Dbg.vrb.println(seg.name.toString());
		if (isSubSegment) {// set inherit attributes and width
			seg.setAttribute(inheritAttributes);
			seg.setWidth(inheritWidth);
			if (seg.name.indexOf('.', 0) != -1) {
				reporter.error(errSyntax,
						"Dots are not allowed in subsegment designators. Sytax error in: "
								+ seg.name.toString() + "\n");
				return null;
			}
		} else {// get width and attributes from Device
			int indexOf = seg.name.indexOf('.', 0);
			HString devName = seg.name.substring(0, indexOf);
			Device dev = MemoryMap.getInstance().getDeviceByName(devName);
			if (dev == null) {
				ErrorReporter.reporter.error(errNoSuchDevice,
						"Device for Segment " + seg.getName().toString()
								+ "not found\n");
			}
			seg.setAttribute(dev.attributes);
			seg.setWidth(dev.width);
		}
		if (sym != sLBrace) {
			nOfErrors++;
			reporter.error(errLBraceExp,
					"expected symbol: {, received symbol: " + symToString()
							+ " ");
			return null;
		}
		next();
		while (sym == sDevice || sym == sAttributes || sym == sBase
				|| sym == sWidth || sym == sSize) {
			switch (sym) {
			case sDevice:
				seg.setDeviceAssignedTo(HString.getHString(deviceAssignment()));
				break;
			case sAttributes:
				seg.setAttributes(attributeAssignment());
				break;
			case sBase:
				seg.setBaseAddress(baseAssignment());
				break;
			case sWidth:
				seg.setWidth(widthAssignment());
				break;
			case sSize:
				seg.setSize(sizeAssignment());
				break;
			}
		}
		while (sym == sSegmentarray || sym == sSegment) {
			if (sym == sSegmentarray) {
				segmentArray(true, seg);
			} else {
				seg.addSubSegment(segment(true, seg.getAttributes(), seg
						.getWidth()));
			}
		}
		if (sym != sRBrace) {
			nOfErrors++;
			reporter.error(errRBraceExp,
					"expected symbol: }, received symbol: " + symToString()
							+ " ");
			return null;
		}
		next();
		return seg;
	}

	private void segmentArray(boolean isSubSegment, Segment parent) {
		int arraySize = 0;
		int baseAddr = -1;
		int width = 0;
		int attributes = 0;
		int nofSegments = 0;

		if (sym != sSegmentarray) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: segmentarray, received symbol: "
							+ symToString() + " ");
			return;
		}
		next();
		HString segName = segmentDesignator();
		Device dev = null;
		if (!isSubSegment) {
			int indexOf = segName.indexOf('.', 0);
			if (indexOf != -1) {
				segName = segName.substring(indexOf + 1);
				HString devName = segName.substring(0, indexOf);
				dev = MemoryMap.getInstance().getDeviceByName(devName);
				if (dev == null) {
					ErrorReporter.reporter.error(errNoSuchDevice,
							"Device " +devName.toString()+" for Segment " + segName.toString()
									+ "not found\n");
					return;
				}
				
					indexOf = segName.indexOf('.', 0);
					if (indexOf != -1) {// it is true when the new Segment is a
						// Subsegment
						HString segment = segName.substring(0, indexOf);
						Segment seg = dev.getSegementByName(segment);
						segName = segName.substring(indexOf + 1);
						indexOf = segName.indexOf('.', 0);
						while (indexOf != -1) {
							segment = segName.substring(0, indexOf);
							seg = seg.getSubSegmentByName(segment);
							segName = segName.substring(indexOf + 1);
							indexOf = segName.indexOf('.', 0);
						}
						parent = seg;
						attributes = seg.getAttributes();
						width = seg.width;
					} else {
						attributes = dev.attributes;
						width = dev.width;
					}
				}else {
					ErrorReporter.reporter.error(errSyntax,
							"Error in memorymap segementarray definition ("
									+ segName.toString()
									+ "), segmentarray names starts with the device name!");
				}
			}
		

		if (sym != sLBrace) {
			nOfErrors++;
			reporter.error(errLBraceExp,
					"expected symbol: {, received symbol: " + symToString()
							+ " ");
			return;
		}
		next();
		int segSize = segmentSizeAssignment();
		while (sym == sArraysize || sym == sWidth || sym == sNofsegements
				|| sym == sBase || sym == sAttributes) {
			if (sym == sArraysize) {
				arraySize = arraySizeAssignment();
			} else if (sym == sNofsegements) {
				nofSegments = nofSegmentAssignment();
			} else if (sym == sWidth) {
				width = widthAssignment();
			} else if (sym == sAttributes) {
				attributes = attributeAssignment();
			} else {// sym == sBase
				baseAddr = baseAssignment();
			}
		}
		if (sym != sRBrace) {
			nOfErrors++;
			reporter.error(errLBraceExp,
					"expected symbol: }, received symbol: " + symToString()
							+ " ");
			return;
		}
		if (arraySize != 0) {
			if (nofSegments != 0) {
				if (nofSegments != (arraySize / segSize)) {
					reporter
							.error(errInconsistentattributes,
									"Number of segemts in segmentarray creation not as expected");
					return;
				}
			} else {
				nofSegments = arraySize / segSize;
				if (arraySize % segSize != 0) {
					reporter.error(errInconsistentattributes,
							"Segmentsize is not a multiple of Arraysize");
					return;
				}
			}
		} else {
			if (nofSegments == 0) {
				reporter.error(errInconsistentattributes,
						"Missing attribute in segmentarray creation");
				return;
			}
		}
		// from here is nofSegments != 0
		Segment root = new Segment(HString.getHString(segName.toString() + 1), baseAddr,
				segSize, width, attributes);
		Segment current = root;

		for (int i = 2; i <= nofSegments; i++) {
			if (baseAddr != -1) {
				baseAddr += segSize;
			}
			current.next = new Segment(HString.getHString(segName.toString() + i),
					baseAddr, segSize, width, attributes);
			current = current.next;
		}
		next();
		if(parent != null){
			parent.addSubSegment(root);
		}else if(!isSubSegment){
			dev.addSegment(root);
		}else{
			reporter.error(errInvalideParameter,
			"Parent Segment must be given for Subsegmentarrays");
		}
	}

	private void regInit() {
		if (sym != sReginit) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: reginit, received symbol: "
							+ symToString() + " ");
			return;
		}
		next();
		if (sym != sLBrace) {
			nOfErrors++;
			reporter.error(errLBraceExp,
					"expected symbol: {, received symbol: " + symToString()
							+ " ");
			return;
		}
		next();
		while (sym == sDesignator) {
			Configuration.setRegInit(HString.getHString(strBuffer),
					regAssignment());
		}
		if (sym != sRBrace) {
			nOfErrors++;
			reporter.error(errRBraceExp,
					"expected symbol: }, received symbol: " + symToString()
							+ " ");
			return;
		}
		next();
	}

	// If modules are called from MemoryMap, targetConfig = null;
	private void modules(TargetConfiguration targetConfig) {
		if (sym != sModules) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: modules, received symbol: "
							+ symToString() + " ");
			return;
		}
		next();
		if (sym != sLBrace) {
			nOfErrors++;
			reporter.error(errLBraceExp,
					"expected symbol: {, received symbol: " + symToString()
							+ " ");
			return;
		}
		next();
		MemoryMap memMap = MemoryMap.getInstance();
		while (sym == sKernel || sym == sInterrupt || sym == sException
				|| sym == sHeap || sym == sDesignator || sym == sDefault) {
			Module root = null, current = null;
			do {
				if (sym == sComma) {// breaks the endless loop
					next();
				}
				switch (sym) {
				case sKernel:
					if (root == null) {
						root = new Module(HString.getHString("kernel"));
						current = root;
					} else {
						current.next = new Module(HString.getHString("kernel"));
						current = current.next;
					}
					next();
					break;
				case sInterrupt:
					if (root == null) {
						root = new Module(HString.getHString("interrupt"));
						current = root;
					} else {
						current.next = new Module(HString
								.getHString("interrupt"));
						current = current.next;
					}
					next();
					break;
				case sException:
					if (root == null) {
						root = new Module(HString.getHString("exception"));
						current = root;
					} else {
						current.next = new Module(HString
								.getHString("exception"));
						current = current.next;
					}
					next();
					break;
				case sHeap:
					if (root == null) {
						root = new Module(HString.getHString("heap"));
						current = root;
					} else {
						current.next = new Module(HString.getHString("heap"));
						current = current.next;
					}
					next();
					break;
				case sDefault:
					if (root == null) {
						root = new Module(HString.getHString("default"));
						current = root;
					} else {
						current.next = new Module(HString.getHString("default"));
						current = current.next;
					}
					next();
					break;
				case sDesignator:
					if (root == null) {
						root = new Module(HString
								.getHString(concatenatedDesignator()));
						current = root;
					} else {
						current.next = new Module(HString
								.getHString(concatenatedDesignator()));
						current = current.next;
					}
					break;
				default:
					reporter.error(errUnexpectetSymExp, "Unexpected symbol: "
							+ symToString() + " by module creation");
					break;
				}
			} while (sym == sComma);
			if (sym != sColon) {
				nOfErrors++;
				reporter.error(errSemicolonMissExp,
						"expected symbol: :, received symbol: " + symToString()
								+ " ");
				return;
			}
			do {
				current = root;
				SegmentAssignment assign = null;
				next();
				switch (sym) {
				case sConst:
					assign = new SegmentAssignment(HString.getHString("const"));
					next();
					break;
				case sCode:
					assign = new SegmentAssignment(HString.getHString("code"));
					next();
					break;
				case sVar:
					assign = new SegmentAssignment(HString.getHString("var"));
					next();
					break;
				case sHeap:
					assign = new SegmentAssignment(HString.getHString("heap"));
					next();
					break;
				case sStack:
					assign = new SegmentAssignment(HString.getHString("stack"));
					next();
					break;
				default:
					nOfErrors++;
					reporter.error(errUnexpectetSymExp,
							"expected symbol: contentattribute, received symbol: "
									+ symToString() + " ");
					return;
				}
				if (sym != sAt) {
					nOfErrors++;
					reporter.error(errAssignExp,
							"expected symbol: @, received symbol: "
									+ symToString() + " ");
					return;
				}
				next();
				assign.setSegmentDesignator(segmentDesignator());
				while (current != null) {
					current.setSegmentAssignment(assign);
					current = current.next;
				}

			} while (sym == sComma);

			if (sym != sSemicolon) {
				nOfErrors++;
				reporter.error(errSemicolonMissExp,
						"expected symbol: ;, received symbol: " + symToString()
								+ " ");
				return;
			}
			next();
			if (targetConfig != null) {
				while (root != null) {
					targetConfig.setModule(root);
					root = root.next;
				}
			} else {
				while (root != null) {
					memMap.setModule(root);
					root = root.next;
				}
			}
		}
		if (sym != sRBrace) {
			nOfErrors++;
			reporter.error(errRBraceExp,
					"expected symbol: }, received symbol: " + symToString()
							+ " ");
			return;
		}
		next();
	}

	private Register register() {
		if (sym != sRegister) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: register, received symbol: "
							+ symToString() + " ");
			return null;
		}
		next();
		if (sym != sDesignator) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: designator, received symbol: "
							+ symToString() + " ");
			return null;
		}
		Register reg = new Register(HString.getHString(strBuffer));
		next();
		if (sym != sLBrace) {
			nOfErrors++;
			reporter.error(errLBraceExp,
					"expected symbol: {, received symbol: " + symToString()
							+ " ");
			return null;
		}
		next();
		while (sym == sType || sym == sAddr || sym == sSize || sym == sRepr) {
			if (sym == sType) {
				reg.setType(regTypeAssignment());
			}
			if (sym == sAddr) {
				reg.setAddress(addressAssignment());
			}
			if (sym == sSize) {
				reg.setSize(sizeAssignment());
			}
			if (sym == sRepr) {
				reg.setRepresentation(registerRepresentationAssignment());
			}
		}
		if (reg.addr < 0 || reg.type < 0 || reg.size < 0) {
			reporter.error(errInconsistentattributes,
					"Missing attribute in creation of Register: "
							+ reg.getName().toString() + "\n");
		}
		if (sym != sRBrace) {
			nOfErrors++;
			reporter.error(errRBraceExp,
					"expected symbol: }, received symbol: " + symToString()
							+ " ");
			return null;
		}
		next();
		return reg;
	}

	private void registermap() {
		if (sym != sRegistermap) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: registermap, received symbol: "
							+ symToString() + " ");
			return;
		}
		next();
		RegisterMap regMap = RegisterMap.getInstance();
		if (sym != sLBrace) {
			nOfErrors++;
			reporter.error(errLBraceExp,
					"expected symbol: {, received symbol: " + symToString()
							+ " ");
			return;
		}
		next();
		while (sym == sRegister) {
			regMap.addRegister(register());
		}
		if (sym != sRBrace) {
			nOfErrors++;
			reporter.error(errRBraceExp,
					"expected symbol: }, received symbol: " + symToString()
							+ " ");
			return;
		}
		next();
	}

	private void targetconfiguration() {
		if (sym != sTargetConf) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: targetconfiguration, received symbol: "
							+ symToString() + " ");
			return;
		}
		next();
		if (sym != sDesignator) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: designator, received symbol: "
							+ symToString() + " ");
			return;
		}
		TargetConfiguration targetConfig = new TargetConfiguration(HString
				.getHString(strBuffer));
		next();
		if (sym != sLBrace) {
			nOfErrors++;
			reporter.error(errLBraceExp,
					"expected symbol: {, received symbol: " + symToString()
							+ " ");
			return;
		}
		next();
		modules(targetConfig);
		if (sym != sRBrace) {
			nOfErrors++;
			reporter.error(errRBraceExp,
					"expected symbol: }, received symbol: " + symToString()
							+ " ");
			return;
		}
		Configuration.addTargetConfiguration(targetConfig);
		next();
	}

	private void project() {
		if (sym != sProject) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: project, received symbol: "
							+ symToString() + " ");
			return;
		}
		next();
		if (sym != sLBrace) {
			nOfErrors++;
			reporter.error(errLBraceExp,
					"expected symbol: {, received symbol: " + symToString()
							+ " ");
			return;
		}
		next();
		Project proj = new Project();
		while (sym == sRootclasses || sym == sLibPath || sym == sTargetConf
				|| sym == sDebugLevel || sym == sPrintLevel) {
			if (sym == sRootclasses) {
				HString classes = rootClassesAssignment();
				proj.setRootClasses(classes);
			} else if (sym == sLibPath) {
				proj.setLibPath(libPathAssignment());
			} else if (sym == sTargetConf) {
				proj.setTagetConfig(targetConfigurationAssignment());
			} else if (sym == sDebugLevel) {
				proj.setDebugLevel(debugLevelAssignment());
			} else if (sym == sPrintLevel) {
				proj.setPrintLevel(printLevelAssignment());
			}
		}
		if (sym != sRBrace) {
			nOfErrors++;
			reporter.error(errRBraceExp,
					"expected symbol: }, received symbol: " + symToString()
							+ " ");
			return;
		}
		Configuration.setProject(proj);
		if (importList != null) {
			for (int i = 0; i < importList.size(); i++) {
				String toCmp = importList.get(i);
				if (!importedFiles.contains(toCmp)) {
					importedFiles.add(toCmp);
					File f = new File(loc + toCmp);
					if (f.exists()) {
						parseAndCreateConfig(loc, toCmp);
					} else {
						parseAndCreateConfig(libPath, toCmp);
					}
				}
			}
			// delete importList to prevent to do imports twice(in "project()")
			importList = null;
		}
		next();
	}

	private void operatingSystem() {
		if (sym != sOperatingSystem) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: operatingsystem, received symbol: "
							+ symToString() + " ");
			return;
		}
		next();
		if (sym != sLBrace) {
			nOfErrors++;
			reporter.error(errLBraceExp,
					"expected symbol: {, received symbol: " + symToString()
							+ " ");
			return;
		}
		next();
		OperatingSystem os = new OperatingSystem();
		while (sym == sKernel || sym == sHeap || sym == sInterrupt
				|| sym == sException) {
			if (sym == sKernel) {
				os.setKernel(kernelAssignment());
			} else if (sym == sHeap) {
				os.setHeap(heapAssignment());
			} else if (sym == sInterrupt) {
				os.setInterrupt(interruptAssignment());
			} else {
				os.setException(exceptionAssignment());
			}
		}
		if (sym != sRBrace) {
			nOfErrors++;
			reporter.error(errRBraceExp,
					"expected symbol: }, received symbol: " + symToString()
							+ " ");
			return;
		}
		Configuration.setOperatingSystem(os);
		next();
	}

	private String versionAssignment() {
		String s;
		if (sym != sVersion) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: version, received symbol: "
							+ symToString() + " ");
			return "";
		}
		next();
		if (sym != sEqualsSign) {
			nOfErrors++;
			reporter.error(errAssignExp,
					"expected symbol: =, received symbol: " + symToString()
							+ " ");
			return "";
		}
		next();
		s = readString();
		if (sym != sSemicolon) {
			nOfErrors++;
			reporter.error(errSemicolonMissExp,
					"expected symbol: ;, received symbol: " + symToString()
							+ " ");
			return s;
		}
		next();
		return s;
	}

	private String fileDescAssignment() {
		String s;
		if (sym != sDescription) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: description, received symbol: "
							+ symToString() + " ");
			return "";
		}
		next();
		if (sym != sEqualsSign) {
			nOfErrors++;
			reporter.error(errAssignExp,
					"expected symbol: =, received symbol: " + symToString()
							+ " ");
			return "";
		}
		next();
		s = readString();
		if (sym != sSemicolon) {
			nOfErrors++;
			reporter.error(errSemicolonMissExp,
					"expected symbol: ;, received symbol: " + symToString()
							+ " ");
			return s;
		}
		next();
		return s;
	}

	private void importAssignment() {
		if (sym != sImport) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: import, received symbol: "
							+ symToString() + " ");
			return;
		}
		next();
		if (sym != sEqualsSign) {
			nOfErrors++;
			reporter.error(errAssignExp,
					"expected symbol: =, received symbol: " + symToString()
							+ " ");
			return;
		}
		do {
			next();
			importList.add(readString());
		} while (sym == sComma);
		if (sym != sSemicolon) {
			nOfErrors++;
			reporter.error(errSemicolonMissExp,
					"expected symbol: ;, received symbol: " + symToString()
							+ " ");
			return;
		}
		next();
		return;
	}

	private int varAssignment() {
		int res = Integer.MIN_VALUE;
		if (sym != sDesignator) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: designator, received symbol: "
							+ symToString() + " ");
			return res;
		}
		next();
		if (sym != sEqualsSign) {
			nOfErrors++;
			reporter.error(errAssignExp,
					"expected symbol: =, received symbol: " + symToString()
							+ " ");
			return res;
		}
		next();
		res = expression();

		if (sym != sSemicolon) {
			nOfErrors++;
			reporter.error(errSemicolonMissExp,
					"expected symbol: ;, received symbol: " + symToString()
							+ " ");
			return res;
		}
		next();
		return res;

	}

	private int expression() {
		int value = term();
		while (sym == sPlus || sym == sMinus) {
			int operator = sym;
			next();
			if (operator == sPlus) {
				value = value + term();
			} else if (operator == sMinus) {
				value = value - term();
			}
		}
		return value;
	}

	private int term() {
		int value = factor();
		while (sym == sMul || sym == sDiv) {
			int operator = sym;
			next();
			if (operator == sMul) {
				value *= factor();
			} else if (operator == sDiv) {
				value /= factor();
			}
		}
		return value;
	}

	private int factor() {
		int value = 1;
		if (sym == sNumber) {
			value = intNumber;
			next();
		} else if (sym == sLParen) {
			next();
			value = expression();
			if (sym == sRParen) {
				next();
			} else {
				nOfErrors++;
				reporter.error(errRParenExp,
						"expected symbol: ), received symbol: " + symToString()
								+ " ");
			}
		} else if (sym == sDesignator) {
			value = Configuration.getValueFor(HString.getHString(strBuffer));
			next();
		} else {
			nOfErrors++;
			reporter.error(errDigitExp,
					"expected symbol: number, received symbol: "
							+ symToString() + " ");
		}
		return value;
	}

	private String deviceAssignment() {
		String s = "";
		if (sym != sDevice) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: device, received symbol: "
							+ symToString() + " ");
			return s;
		}
		next();
		if (sym != sEqualsSign) {
			nOfErrors++;
			reporter.error(errAssignExp,
					"expected symbol: =, received symbol: " + symToString()
							+ " ");
			return s;
		}
		next();
		if (sym != sDesignator) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: designator, received symbol: "
							+ symToString() + " ");
			return s;
		}
		s = strBuffer;
		next();
		if (sym != sSemicolon) {
			nOfErrors++;
			reporter.error(errSemicolonMissExp,
					"expected symbol: ;, received symbol: " + symToString()
							+ " ");
			return s;
		}
		next();
		return s;
	}

	private int baseAssignment() {
		int res = Integer.MIN_VALUE;
		if (sym != sBase) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: base, received symbol: " + symToString()
							+ " ");
			return res;
		}
		next();
		if (sym != sEqualsSign) {
			nOfErrors++;
			reporter.error(errAssignExp,
					"expected symbol: =, received symbol: " + symToString()
							+ " ");
			return res;
		}
		next();
		res = expression();

		if (sym != sSemicolon) {
			nOfErrors++;
			reporter.error(errSemicolonMissExp,
					"expected symbol: ;, received symbol: " + symToString()
							+ " ");
			return res;
		}
		next();
		return res;
	}

	private int widthAssignment() {
		int res = Integer.MIN_VALUE;
		if (sym != sWidth) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: width, received symbol: " + symToString()
							+ " ");
			return res;
		}
		next();
		if (sym != sEqualsSign) {
			nOfErrors++;
			reporter.error(errAssignExp,
					"expected symbol: =, received symbol: " + symToString()
							+ " ");
			return res;
		}
		next();
		res = expression();

		if (sym != sSemicolon) {
			nOfErrors++;
			reporter.error(errSemicolonMissExp,
					"expected symbol: ;, received symbol: " + symToString()
							+ " ");
			return res;
		}
		next();
		return res;
	}

	private int sizeAssignment() {
		int res = Integer.MIN_VALUE;
		if (sym != sSize) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: size, received symbol: " + symToString()
							+ " ");
			return res;
		}
		next();
		if (sym != sEqualsSign) {
			nOfErrors++;
			reporter.error(errAssignExp,
					"expected symbol: =, received symbol: " + symToString()
							+ " ");
			return res;
		}
		next();
		res = expression();

		if (sym != sSemicolon) {
			nOfErrors++;
			reporter.error(errSemicolonMissExp,
					"expected symbol: ;, received symbol: " + symToString()
							+ " ");
			return res;
		}
		next();
		return res;
	}

	private int segmentSizeAssignment() {
		int res = Integer.MIN_VALUE;
		if (sym != sSegmentsize) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: segmentsize, received symbol: "
							+ symToString() + " ");
			return res;
		}
		next();
		if (sym != sEqualsSign) {
			nOfErrors++;
			reporter.error(errAssignExp,
					"expected symbol: =, received symbol: " + symToString()
							+ " ");
			return res;
		}
		next();
		res = expression();

		if (sym != sSemicolon) {
			nOfErrors++;
			reporter.error(errSemicolonMissExp,
					"expected symbol: ;, received symbol: " + symToString()
							+ " ");
			return res;
		}
		next();
		return res;
	}

	private int arraySizeAssignment() {
		int res = Integer.MIN_VALUE;
		if (sym != sArraysize) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: arraysize, received symbol: "
							+ symToString() + " ");
			return res;
		}
		next();
		if (sym != sEqualsSign) {
			nOfErrors++;
			reporter.error(errAssignExp,
					"expected symbol: =, received symbol: " + symToString()
							+ " ");
			return res;
		}
		next();
		res = expression();

		if (sym != sSemicolon) {
			nOfErrors++;
			reporter.error(errSemicolonMissExp,
					"expected symbol: ;, received symbol: " + symToString()
							+ " ");
			return res;
		}
		next();
		return res;
	}

	private int nofSegmentAssignment() {
		int res = Integer.MIN_VALUE;
		if (sym != sNofsegements) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: nofsegments, received symbol: "
							+ symToString() + " ");
			return res;
		}
		next();
		if (sym != sEqualsSign) {
			nOfErrors++;
			reporter.error(errAssignExp,
					"expected symbol: =, received symbol: " + symToString()
							+ " ");
			return res;
		}
		next();
		res = expression();

		if (sym != sSemicolon) {
			nOfErrors++;
			reporter.error(errSemicolonMissExp,
					"expected symbol: ;, received symbol: " + symToString()
							+ " ");
			return res;
		}
		next();
		return res;
	}

	private int regAssignment() {
		return varAssignment();
	}

	private int attributeAssignment() {
		int res = 0;
		if (sym != sAttributes) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: attributes, received symbol: "
							+ symToString() + " ");
			return res;
		}
		next();
		if (sym != sEqualsSign) {
			nOfErrors++;
			reporter.error(errAssignExp,
					"expected symbol: =, received symbol: " + symToString()
							+ " ");
			return res;
		}
		do {
			next();
			switch (sym) {
			case sRead:
				res |= (1 << atrRead);
				break;
			case sWrite:
				res |= (1 << atrWrite);
				break;
			case sConst:
				res |= (1 << atrWconst);
				break;
			case sCode:
				res |= (1 << atrCode);
				break;
			case sVar:
				res |= (1 << atrVar);
				break;
			case sHeap:
				res |= (1 << atrHeap);
				break;
			case sStack:
				res |= (1 << atrStack);
				break;
			case sSysTab:
				res |= (1 << AtrSysTab);
				break;
			default:
				nOfErrors++;
				reporter.error(errUnexpectetSymExp,
						"expected symbol: attributes, received symbol: "
								+ symToString() + " ");
				return res;
			}
			next();
		} while (sym == sComma);
		if (sym != sSemicolon) {
			nOfErrors++;
			reporter.error(errSemicolonMissExp,
					"expected symbol: ;, received symbol: " + symToString()
							+ " ");
			return res;
		}
		next();
		return res;
	}

	private int regTypeAssignment() {
		int s;
		if (sym != sType) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: type, received symbol: " + symToString()
							+ " ");
			return sUndef;
		}
		next();
		if (sym != sEqualsSign) {
			nOfErrors++;
			reporter.error(errAssignExp,
					"expected symbol: =, received symbol: " + symToString()
							+ " ");
			return sUndef;
		}
		next();
		if (sym == sGPR || sym == sFPR || sym == sSPR) {
			s = sym;
		} else {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: gpr | fpr | spr, received symbol: "
							+ symToString() + " ");
			return sUndef;
		}
		next();
		if (sym != sSemicolon) {
			nOfErrors++;
			reporter.error(errSemicolonMissExp,
					"expected symbol: ;, received symbol: " + symToString()
							+ " ");
			return s;
		}
		next();
		return s;
	}

	private int registerRepresentationAssignment() {
		int s = sUndef;
		if (sym != sRepr) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: repr, received symbol: " + symToString()
							+ " ");
			return s;
		}
		next();
		if (sym != sEqualsSign) {
			nOfErrors++;
			reporter.error(errAssignExp,
					"expected symbol: =, received symbol: " + symToString()
							+ " ");
			return s;
		}
		next();
		if (sym != sDez && sym != sBin && sym != sHex) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: dez | bin | hex, received symbol: "
							+ symToString() + " ");
			return s;
		}
		s = sym;
		next();
		if (sym != sSemicolon) {
			nOfErrors++;
			reporter.error(errSemicolonMissExp,
					"expected symbol: ;, received symbol: " + symToString()
							+ " ");
			return s;
		}
		next();
		return s;
	}

	private int addressAssignment() {
		int res = Integer.MIN_VALUE;
		if (sym != sAddr) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: addr, received symbol: " + symToString()
							+ " ");
			return res;
		}
		next();
		if (sym != sEqualsSign) {
			nOfErrors++;
			reporter.error(errAssignExp,
					"expected symbol: =, received symbol: " + symToString()
							+ " ");
			return res;
		}
		next();
		res = expression();

		if (sym != sSemicolon) {
			nOfErrors++;
			reporter.error(errSemicolonMissExp,
					"expected symbol: ;, received symbol: " + symToString()
							+ " ");
			return res;
		}
		next();
		return res;
	}

	private String concatenatedDesignator() {
		StringBuffer sb = new StringBuffer();
		if (sym != sDesignator) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: designator, received symbol: "
							+ symToString() + " ");
			return sb.toString();
		}
		sb.append(strBuffer);
		next();
		while (sym == sDot) {
			sb.append(".");
			next();
			if (sym == sMul) {
				sb.append("*");
			} else if (sym != sDesignator) {
				nOfErrors++;
				reporter.error(errUnexpectetSymExp,
						"expected symbol: designator, received symbol: "
								+ symToString() + " ");
				return sb.toString();
			} else {
				sb.append(strBuffer);
			}
			next();
		}
		return sb.toString();
	}

	private HString segmentDesignator() {
		StringBuffer sb = new StringBuffer();
		if (sym != sDesignator) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: designator, received symbol: "
							+ symToString() + " ");
			return HString.getHString(sb.toString());
		}
		sb.append(strBuffer);
		next();
		while (sym == sDot) {
			next();
			if (sym != sDesignator) {
				nOfErrors++;
				reporter.error(errUnexpectetSymExp,
						"expected symbol: designator, received symbol: "
								+ symToString() + " ");
				return HString.getHString(sb.toString());
			}
			sb.append(".");
			sb.append(strBuffer);
			next();
		}
		return HString.getHString(sb.toString());
	}

	private HString rootClassesAssignment() {
		HString tempList = null;
		HString current = null;
		if (sym != sRootclasses) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected: rootclasses, received symbol: " + symToString()
							+ " ");
			return tempList;
		}
		next();
		if (sym != sEqualsSign) {
			nOfErrors++;
			reporter.error(errAssignExp, "expected: =, received symbol: "
					+ symToString() + " ");
			return tempList;
		}
		do {
			next();
			if (tempList == null) {
				tempList = HString.getHString(readString());
				current = tempList;
			} else {
				current.next = HString.getHString(readString());
				current = current.next;
			}
		} while (sym == sComma);
		if (sym != sSemicolon) {
			nOfErrors++;
			reporter.error(errSemicolonMissExp,
					"expected symbol: ;, received symbol: " + symToString()
							+ " ");
			return tempList;
		}
		next();
		return tempList;
	}

	private HString libPathAssignment() {
		HString s = HString.getHString("");
		if (sym != sLibPath) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected: libpath, received symbol: " + symToString()
							+ " ");
			return s;
		}
		next();
		if (sym != sEqualsSign) {
			nOfErrors++;
			reporter.error(errAssignExp, "expected: =, received symbol: "
					+ symToString() + " ");
			return s;
		}
		next();
		s = HString.getHString(readString());
		if (sym != sSemicolon) {
			nOfErrors++;
			reporter.error(errSemicolonMissExp,
					"expected symbol: ;, received symbol: " + symToString()
							+ " ");
			return s;
		}
		next();
		return s;
	}

	private HString targetConfigurationAssignment() {
		HString s = HString.getHString("");
		if (sym != sTargetConf) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected: targetconfiguration , received symbol: "
							+ symToString() + " ");
			return s;
		}
		next();
		if (sym != sEqualsSign) {
			nOfErrors++;
			reporter.error(errAssignExp, "expected: =, received symbol: "
					+ symToString() + " ");
			return s;
		}
		next();
		if (sym != sDesignator) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected symbol: designator, received symbol: "
							+ symToString() + " ");
			return s;
		}
		s = HString.getHString(strBuffer);
		next();
		if (sym != sSemicolon) {
			nOfErrors++;
			reporter.error(errSemicolonMissExp,
					"expected symbol: ;, received symbol: " + symToString()
							+ " ");
			return s;
		}
		next();
		return s;
	}

	private HString kernelAssignment() {
		HString s = HString.getHString("");
		if (sym != sKernel) {
			nOfErrors++;
			reporter
					.error(errUnexpectetSymExp,
							"expected: kernel, received symbol: "
									+ symToString() + " ");
			return s;
		}
		next();
		if (sym != sEqualsSign) {
			nOfErrors++;
			reporter.error(errAssignExp, "expected: =, received symbol: "
					+ symToString() + " ");
			return s;
		}
		next();
		s = HString.getHString(readString());
		if (sym != sSemicolon) {
			nOfErrors++;
			reporter.error(errSemicolonMissExp,
					"expected symbol: ;, received symbol: " + symToString()
							+ " ");
			return s;
		}
		next();
		return s;
	}

	private HString heapAssignment() {
		HString s = HString.getHString("");
		if (sym != sHeap) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected: heap, received symbol: " + symToString() + " ");
			return s;
		}
		next();
		if (sym != sEqualsSign) {
			nOfErrors++;
			reporter.error(errAssignExp, "expected: =, received symbol: "
					+ symToString() + " ");
			return s;
		}
		next();
		s = HString.getHString(readString());
		if (sym != sSemicolon) {
			nOfErrors++;
			reporter.error(errSemicolonMissExp,
					"expected symbol: ;, received symbol: " + symToString()
							+ " ");
			return s;
		}
		next();
		return s;
	}

	private HString interruptAssignment() {
		HString s = HString.getHString("");
		if (sym != sInterrupt) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp,
					"expected: interrupt, received symbol: " + symToString()
							+ " ");
			return s;
		}
		next();
		if (sym != sEqualsSign) {
			nOfErrors++;
			reporter.error(errAssignExp, "expected: =, received symbol: "
					+ symToString() + " ");
			return s;
		}
		next();
		s = HString.getHString(readString());
		if (sym != sSemicolon) {
			nOfErrors++;
			reporter.error(errSemicolonMissExp,
					"expected symbol: ;, received symbol: " + symToString()
							+ " ");
			return s;
		}
		next();
		return s;
	}

	private HString exceptionAssignment() {
		HString s = HString.getHString("");
		if (sym != sException) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp, "expected: exception"
					+ ", received symbol: " + symToString() + " ");
			return s;
		}
		next();
		if (sym != sEqualsSign) {
			nOfErrors++;
			reporter.error(errAssignExp, "expected: =, received symbol: "
					+ symToString() + " ");
			return s;
		}
		next();
		s = HString.getHString(readString());
		if (sym != sSemicolon) {
			nOfErrors++;
			reporter.error(errSemicolonMissExp,
					"expected symbol: ;, received symbol: " + symToString()
							+ " ");
			return s;
		}
		next();
		return s;
	}

	private int debugLevelAssignment() {
		int res = Integer.MIN_VALUE;
		if (sym != sDebugLevel) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp, "expected: exception"
					+ ", received symbol: " + symToString() + " ");
			return res;
		}
		next();
		if (sym != sEqualsSign) {
			nOfErrors++;
			reporter.error(errAssignExp, "expected: =, received symbol: "
					+ symToString() + " ");
			return res;
		}
		next();
		if (sym != sNumber) {
			nOfErrors++;
			reporter.error(errDigitExp, "expected: number, received symbol: "
					+ symToString() + " ");
			return res;
		}
		res = intNumber;
		next();
		if (sym != sSemicolon) {
			nOfErrors++;
			reporter.error(errSemicolonMissExp,
					"expected symbol: ;, received symbol: " + symToString()
							+ " ");
			return res;
		}
		next();
		return res;
	}

	private int printLevelAssignment() {
		int res = Integer.MIN_VALUE;
		if (sym != sPrintLevel) {
			nOfErrors++;
			reporter.error(errUnexpectetSymExp, "expected: printlevel"
					+ ", received symbol: " + symToString() + " ");
			return res;
		}
		next();
		if (sym != sEqualsSign) {
			nOfErrors++;
			reporter.error(errAssignExp, "expected: =, received symbol: "
					+ symToString() + " ");
			return res;
		}
		next();
		if (sym != sNumber) {
			nOfErrors++;
			reporter.error(errDigitExp, "expected: number, received symbol: "
					+ symToString() + " ");
			return res;
		}
		res = intNumber;
		next();
		if (sym != sSemicolon) {
			nOfErrors++;
			reporter.error(errSemicolonMissExp,
					"expected symbol: ;, received symbol: " + symToString()
							+ " ");
			return res;
		}
		next();
		return res;
	}

}