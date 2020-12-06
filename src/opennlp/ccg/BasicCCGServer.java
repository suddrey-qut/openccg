///////////////////////////////////////////////////////////////////////////////
// Copyright (C) 2003-4 Jason Baldridge, Gann Bierner,
//                      University of Edinburgh (Michael White),
//                      Alexandros Triantafyllidis and David Reitter
// Copyright (C) 2006 Ben Wing
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//////////////////////////////////////////////////////////////////////////////

package opennlp.ccg.services;

import opennlp.ccg.lexicon.*;
import opennlp.ccg.grammar.*;
import opennlp.ccg.parse.*;
import opennlp.ccg.util.*;
import opennlp.ccg.synsem.*;
import opennlp.ccg.realize.*;
import opennlp.ccg.hylo.*;
import opennlp.ccg.ngrams.*;
import opennlp.ccg.test.*;
import opennlp.ccg.realize.Edge; // only realization edges referenced (for preferences)

import org.jdom.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.prefs.*;

/**
 * A text interface for testing grammars.
 *
 * @author  Jason Baldridge
 * @author  Gann Bierner
 * @author  Michael White
 * @author  Alexandros Triantafyllidis
 * @author  David Reitter
 * @version $Revision: 1.67 $, $Date: 2011/12/13 04:00:54 $
 */
public class BasicCCGServer {
    /** Main method for tccg. */
    static boolean logging = false;

    @SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException, LexException {

        String usage = "java opennlp.ccg.TextCCG " +
                       "(<grammarfile>) | (-exportprefs <prefsfile>) | (-importprefs <prefsfile>)";

        if (args.length > 0 && args[0].equals("-h")) {
            System.out.println("Usage: " + usage);
            System.exit(0);
        }

        // args
        String grammarfile = "grammar.xml";
        String prefsfile = null;
        boolean exportPrefs = false;
        boolean importPrefs = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-exportprefs")) {
                exportPrefs = true; prefsfile = args[++i]; continue;
            }
            if (args[i].equals("-importprefs")) {
                importPrefs = true; prefsfile = args[++i]; continue;
            }
            if (args[i].equals("-logging")) {
                BasicCCGServer.logging = true; continue;
            }
            grammarfile = args[i];
        }

        // prefs
        Preferences prefs = Preferences.userNodeForPackage(BasicCCGServer.class);
        try {
            if (exportPrefs) {
                BasicCCGServer.log("Exporting preferences to prefsfile: " + prefsfile);
                prefs.exportNode(new FileOutputStream(prefsfile));
                return;
            }
            if (importPrefs) {
                BasicCCGServer.log("Importing preferences from prefsfile: " + prefsfile);
                Preferences.importPreferences(new FileInputStream(prefsfile));
                return;
            }
        } catch (Exception exc) {
            throw (IOException) new IOException().initCause(exc);
        }

        // load grammar
        URL grammarURL = new File(grammarfile).toURI().toURL();
        BasicCCGServer.log("Loading grammar from URL: " + grammarURL);
        Grammar grammar = new Grammar(grammarURL);

		if (grammar.getName() != null)
			BasicCCGServer.log("Grammar '" + grammar.getName() + "' loaded.");

        BasicCCGServer.log();

        // create parser and realizer
        Parser parser = new Parser(grammar);
        Realizer realizer = new Realizer(grammar);

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            String input = reader.readLine();
            String retval = "";

            try {
                input = input.trim();

                parser.parse(input);

                List<Sign> parses = parser.getResult();
                Sign[] results = new Sign[parses.size()];
                parses.toArray(results);
                int resLength = results.length;

                if (resLength == 0)
                    continue;

                for (int i = 0; i < Math.min(500, resLength); i++) {
                    Category cat = results[i].getCategory();
                    LF convertedLF = null;

                    if (cat.getLF() != null) {
                        cat = cat.copy();
                        Nominal index = cat.getIndexNominal();
                        Sign rootSign = results[i]; // could add a switch here for naming convention
                        convertedLF = HyloHelper.compactAndConvertNominals(cat.getLF(), index, rootSign);
                        cat.setLF(null);
                    }

                    ByteArrayOutputStream bstream = new ByteArrayOutputStream();
                    grammar.saveToXml(convertedLF, input, bstream);

                    retval += bstream.toString().replaceAll("(\\r|\\n|\\t)", "");
                }
            } catch (Exception e) {
            }
            System.out.println(retval);
        }

    }

    static void log(String msg) {
        if (logging) {
            System.out.println(msg);
        }
    }
    static void log() {
        if (logging) {
            System.out.println();
        }
    }
}
