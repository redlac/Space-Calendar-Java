package com.caldertrombley.spacecalendar;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.time.Month;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Scanner;
import java.util.zip.ZipInputStream;

import javax.servlet.http.*;

import com.google.gson.Gson;

/*Parse the raw astronomy data and encode as json, so that it can be read by JavaScript.*/
@SuppressWarnings("serial")
public class SpaceCalendarServlet extends HttpServlet {
	
	private static final int months = 12;
	private HashMap<String, Integer> monthDays;
	private String[] keywords = {"New Moon","Full Moon","Mars","Venus","Mercury","Moon Perigee", "Moon Apogee","Shower","Perihelion","Aphelion","Jupiter","Saturn","Neptune","Uranus","First Quarter","Last Quarter","Moon Descending Node","Moon Ascending Node","Aldebaran","Moon North","Spica","Moon South","Vernal Equinox","Total Lunar Eclipse","Annular Solar Eclipse","Summer Solstice","Autumnal Equinox","Winter Solstice","Antares","Partial Solar Eclipse"};

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		ArrayList<String> astroData = parseAstroFile();
		monthDays = buildMonthDays();
		ArrayList<LinkedHashMap<String, ArrayList<ArrayList<String>>>> eventData = getMonthData(astroData, monthDays);
		ArrayList<LinkedHashMap<String, ArrayList<ArrayList<String>>>> finalEventData = setIcons(eventData);
		//Encode final data in json.
		String json = new Gson().toJson(finalEventData);
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		PrintWriter out = resp.getWriter();
		out.write(json);
	}

	//Read astronomy data from text file into list.
	private ArrayList<String> parseAstroFile() {
		InputStream inputStream = getServletContext().getResourceAsStream("/WEB-INF/astronomydata");
		System.out.println("is"+inputStream);
		ArrayList<String> astroData = new ArrayList<String>();
		try(Scanner scanner = new Scanner(inputStream)){
                while(scanner.hasNextLine()){
                   astroData.add(scanner.nextLine());
                }
		}
        return astroData;
	}
	
	public static String theMonth(int month){
	    String[] monthNames = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
	    return monthNames[month];
	}
	
	//Make a map of the form ("month name" => "number of days in month")
	private HashMap<String, Integer> buildMonthDays() {
		monthDays = new HashMap<String, Integer>();
		for (int i=0; i<12; i++){
			Calendar calendar = new GregorianCalendar(2014, i, 1); 
			String month = theMonth(i).substring(0,3);
			monthDays.put(month, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
		}
		return monthDays;
	}
	
	//Get basic data for each month without icons for calendar.
	private ArrayList<LinkedHashMap<String, ArrayList<ArrayList<String>>>> getMonthData(ArrayList<String> astroDataList, HashMap<String, Integer> months){
		System.out.println("months: " + months);
		String currentMonth = "";
		int lineNum = 0;
		ArrayList<LinkedHashMap<String, ArrayList<ArrayList<String>>>> monthData = new ArrayList<LinkedHashMap<String, ArrayList<ArrayList<String>>>>(); 
		//Get data for each day of each month.
		for (String line: astroDataList){
			String currentLine = astroDataList.get(lineNum);
			if (currentLine.length() > 0){
                                System.out.println(currentLine.indexOf("0"));
                                System.out.println(currentLine.length());
                                String startLine;
                                startLine = currentLine.substring(0, 3); 
                                if (startLine.equals("All")){
                                        break;
                                }else{
                                        if (months.containsKey(startLine)){
                                                currentMonth = startLine;
                                                System.out.println("cm" + currentMonth);
                                        }
                                }
                                LinkedHashMap<String, ArrayList<ArrayList<String>>> monthArray = new LinkedHashMap<String, ArrayList<ArrayList<String>>>();
                                if(currentLine.indexOf(currentMonth) != -1){
                                		System.out.println("cur" + currentMonth);
                                		System.out.println("sl" + currentLine + "el");
                                        String dateBegin = currentLine.substring(4, 6);
                                        String time = currentLine.substring(11, 16);
                                        String event = currentLine.substring(16);
                                        String eventDay = (dateBegin.charAt(0) == '0') ? dateBegin.substring(1,2) : dateBegin;
                                        ArrayList<String> details = new ArrayList<String>();
                                        details.add(eventDay);
                                        details.add(time);
                                        details.add(event);
                                        ArrayList<ArrayList<String>> wrapper = new ArrayList<ArrayList<String>>();
                                        wrapper.add(details);
                                        monthArray.put(currentMonth, wrapper);
                                        monthData.add(monthArray);
                                }else{
                                        String dateBegin = currentLine.substring(2, 4);
                                        System.out.println("db " + dateBegin);
                                        String time = currentLine.substring(9, 14);
                                        String event = currentLine.substring(14);
                                        String eventDay = (String) ((dateBegin.charAt(0) == '0') ? dateBegin.substring(1,2) : dateBegin);
                                        System.out.println("ed" + eventDay);
                                        ArrayList<String> details = new ArrayList<String>();
                                        details.add(eventDay);
                                        details.add(time);
                                        details.add(event);
                                        ArrayList<ArrayList<String>> wrapper = new ArrayList<ArrayList<String>>();
                                        wrapper.add(details);
                                        monthArray.put(currentMonth, wrapper);
                                        monthData.add(monthArray);
                                }
                                System.out.println("ma" + monthArray);
			}
			lineNum++;
		}
		return monthData;
	}
	
	//Put icons for calendar.
	private ArrayList<LinkedHashMap<String, ArrayList<ArrayList<String>>>> setIcons(ArrayList<LinkedHashMap<String, ArrayList<ArrayList<String>>>> eventData){
		ArrayList<LinkedHashMap<String, ArrayList<ArrayList<String>>>> finalEvents = new ArrayList<LinkedHashMap<String, ArrayList<ArrayList<String>>>>();
		for (LinkedHashMap<String, ArrayList<ArrayList<String>>> line: eventData){
			ArrayList<String> iconsArray = checkKeywords(line);
			String firstKey = line.keySet().iterator().next();
			ArrayList<ArrayList<String>> calData = line.get(firstKey);
			calData.add(iconsArray);
			line.put(firstKey, calData);
			finalEvents.add(line);
		}
		System.out.println(finalEvents);
		return finalEvents;
	}
	
	//Add names of icons to main data array, grouped by category.
	private ArrayList<String> checkKeywords(LinkedHashMap<String, ArrayList<ArrayList<String>>> line){
		ArrayList<String> icons = new ArrayList<String>();
		String firstKey = line.keySet().iterator().next();
		ArrayList<String> dayData = line.get(firstKey).get(0);
		for (String word : keywords){
			if (dayData.get(2).trim().contains(word.trim())){
				icons.add(getImageName(word.trim()));
			}
		}
		return icons;
	}
	
	//Get the image associated with each keyword.
	private String getImageName(String word){
		LinkedHashMap<String, ArrayList<String>> autumnKeys = new LinkedHashMap<String, ArrayList<String>>();
		autumnKeys.put("autumn", new ArrayList<String>());
		autumnKeys.get("autumn").add("Autumnal Equinox");
		LinkedHashMap<String, ArrayList<String>> butterflyKeys = new LinkedHashMap<String, ArrayList<String>>();
		butterflyKeys.put("butterfly", new ArrayList<String>());
		butterflyKeys.get("butterfly").add("Vernal Equinox");
		LinkedHashMap<String, ArrayList<String>> cometKeys = new LinkedHashMap<String, ArrayList<String>>();
		cometKeys.put("comet", new ArrayList<String>());
		cometKeys.get("comet").add("Shower");
		LinkedHashMap<String, ArrayList<String>> flowerKeys = new LinkedHashMap<String, ArrayList<String>>();
		flowerKeys.put("flower", new ArrayList<String>());
		flowerKeys.get("flower").add("Summer Solstice");
		LinkedHashMap<String, ArrayList<String>> moonDistanceKeys = new LinkedHashMap<String, ArrayList<String>>();
		moonDistanceKeys.put("moondistance", new ArrayList<String>());
		moonDistanceKeys.get("moondistance").add("Moon Perigee");
		moonDistanceKeys.get("moondistance").add("Moon Apogee");
		LinkedHashMap<String, ArrayList<String>> planetKeys = new LinkedHashMap<String, ArrayList<String>>();
		planetKeys.put("planet", new ArrayList<String>());
		planetKeys.get("planet").add("Mars");
		planetKeys.get("planet").add("Venus");
		planetKeys.get("planet").add("Mercury");
		planetKeys.get("planet").add("Jupiter");
		planetKeys.get("planet").add("Saturn");
		planetKeys.get("planet").add("Neptune");
		planetKeys.get("planet").add("Uranus");
		planetKeys.get("planet").add("Moon Descending Node");
		planetKeys.get("planet").add("Moon Ascending Node");
		planetKeys.get("planet").add("Moon North");
		planetKeys.get("planet").add("Moon South");
		LinkedHashMap<String, ArrayList<String>> snowflakeKeys = new LinkedHashMap<String, ArrayList<String>>();
		snowflakeKeys.put("snowflake", new ArrayList<String>());
		snowflakeKeys.get("snowflake").add("Winter Solstice");
		LinkedHashMap<String, ArrayList<String>> solareclipseKeys = new LinkedHashMap<String, ArrayList<String>>();
		solareclipseKeys.put("solareclipse", new ArrayList<String>());
		solareclipseKeys.get("solareclipse").add("Partial Solar Eclipse");
		solareclipseKeys.get("solareclipse").add("Annular Solar Eclipse");
		LinkedHashMap<String, ArrayList<String>> starKeys = new LinkedHashMap<String, ArrayList<String>>();
		starKeys.put("star", new ArrayList<String>());
		starKeys.get("star").add("Aldebaran");
		starKeys.get("star").add("Spica");
		starKeys.get("star").add("Antares");
		LinkedHashMap<String, ArrayList<String>> sunKeys = new LinkedHashMap<String, ArrayList<String>>();
		sunKeys.put("sun", new ArrayList<String>());
		sunKeys.get("sun").add("Perihelion");
		sunKeys.get("sun").add("Aphelion");
		LinkedHashMap<String, ArrayList<String>> firstQuarterKeys = new LinkedHashMap<String, ArrayList<String>>();
		firstQuarterKeys.put("firstquarter", new ArrayList<String>());
		firstQuarterKeys.get("firstquarter").add("First Quarter");
		LinkedHashMap<String, ArrayList<String>> lastQuarterKeys = new LinkedHashMap<String, ArrayList<String>>();
		lastQuarterKeys.put("lastquarter", new ArrayList<String>());
		lastQuarterKeys.get("lastquarter").add("Last Quarter");
		LinkedHashMap<String, ArrayList<String>> fullMoonKeys = new LinkedHashMap<String, ArrayList<String>>();
		fullMoonKeys.put("fullmoon", new ArrayList<String>());
		fullMoonKeys.get("fullmoon").add("Full Moon");
		fullMoonKeys.get("fullmoon").add("Total Lunar Eclipse");
		LinkedHashMap<String, ArrayList<String>> newMoonKeys = new LinkedHashMap<String, ArrayList<String>>();
		newMoonKeys.put("newmoon", new ArrayList<String>());
		newMoonKeys.get("newmoon").add("New Moon");

		//Put all image(icon) keys in a list.
		ArrayList<LinkedHashMap<String, ArrayList<String>>> imagesKeys = new ArrayList<LinkedHashMap<String,ArrayList<String>>>();
		imagesKeys.add(autumnKeys);
		imagesKeys.add(butterflyKeys);
		imagesKeys.add(cometKeys);
		imagesKeys.add(flowerKeys);
		imagesKeys.add(moonDistanceKeys);
		imagesKeys.add(planetKeys);
		imagesKeys.add(snowflakeKeys);
		imagesKeys.add(solareclipseKeys);
		imagesKeys.add(starKeys);
		imagesKeys.add(sunKeys);
		imagesKeys.add(firstQuarterKeys);
		imagesKeys.add(lastQuarterKeys);
		imagesKeys.add(fullMoonKeys);
		imagesKeys.add(newMoonKeys);
		
		//Get key in map (category).
		return getImageKey(imagesKeys, word);
	}

	private String getImageKey(
			ArrayList<LinkedHashMap<String, ArrayList<String>>> imagesKeys, String word) {
		
			for (LinkedHashMap<String, ArrayList<String>> key : imagesKeys){
				String firstKey = key.keySet().iterator().next();
				ArrayList<String> values = key.get(firstKey);
				for (String val : values){
					if (val.equals(word)){
						return firstKey;
					}
				}
			}
			return null;
	}
}
