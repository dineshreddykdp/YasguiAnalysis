package org.data2semantics.yasgui.analysis;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.data2semantics.yasgui.analysis.helpers.AccessibilityStats;
import org.data2semantics.yasgui.analysis.helpers.AccessibilityStats.EndpointAccessiblityStatus;

import au.com.bytecode.opencsv.CSVWriter;

public class EndpointCollection {
//	
	private HashMap<String, Integer> endpoints = new HashMap<String, Integer>();
	private HashMap<String, Integer> endpointTriplesUsed = null;
	private AccessibilityStats accessibilityStats;
	
	public EndpointCollection(Collection collection) {
		accessibilityStats = new AccessibilityStats(this, collection);
	}
	public AccessibilityStats getAccessibilityStats() {
		return this.accessibilityStats;
	}
	public HashMap<String, Integer> getEndpointForDomain(String domain) {
		HashMap<String, Integer> foundEndpoint = null;
		for (String endpoint: endpoints.keySet()) {
			if (endpoint.contains(domain)) {
				foundEndpoint = new HashMap<String, Integer>();
				foundEndpoint.put(endpoint, endpoints.get(endpoint));
				break;
			}
		}
		return foundEndpoint;
	}
	public void addEndpoint(String endpoint) {
		addEndpoint(endpoint, 1);
	}
	
	public void addEndpoint(String endpoint, int count) {
		if (endpoints.containsKey(endpoint)) count += endpoints.get(endpoint).intValue();
		endpoints.put(endpoint, count);
	}
	
	public HashMap<String, Integer> getEndpoints() {
		return this.endpoints;
	}
	
	public int getTotalCount() {
		int count = 0;
		for (Integer endpointCount: endpoints.values()) {
			count += endpointCount.intValue();
		}
		return count;
	}
	
	public int getEndpointCount(String endpoint) {
		int count = 0;
		if (this.endpoints.containsKey(endpoint)) {
			count = endpoints.get(endpoint).intValue();
		}
		return count;
	}
	public HashMap<String, Integer> getEndpoints(int minEndpointCount) {
		HashMap<String, Integer> newEndpointsList = new HashMap<String, Integer>();
		for (Map.Entry<String, Integer> entry : endpoints.entrySet()) {
		    if (entry.getValue() >= minEndpointCount) {
		    	newEndpointsList.put(entry.getKey(), entry.getValue());
		    }
		}
		return newEndpointsList;
	}
	public int getTotalCount(int minEndpointCount) {
		int count = 0;
		for (Integer num: getEndpoints(minEndpointCount).values()) {
			count += num;
		}
		return count;
	}
	public void calcAggregatedStats(String name) throws IOException, ParseException {
//		this.ckanStats.calc(name);
		System.out.println("calc aggregated endpoint stats");
		this.accessibilityStats.calc(name);
	}
	
	
	
	public String toString() {
		return "total endpoint count: " + getTotalCount() + " distinct endpoint count: " + getEndpoints().size();
	}
	

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map<String, Integer> getOrderedEndpoints() {
		List list = new LinkedList(endpoints.entrySet());
		 
		// sort list based on comparator
		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Comparable) ((Map.Entry) (o1)).getValue())
                                       .compareTo(((Map.Entry) (o2)).getValue());
			}
		});
 
		// put sorted list into map again
                //LinkedHashMap make sure order in which keys were inserted
		Map sortedMap = new LinkedHashMap();
		for (Iterator it = list.iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		
        return sortedMap;
	}

	public void toCsv(String name) throws IOException {
		toSimpleStatsCsv(name);
		toElaborateStatsCsv(name);
		
	}
	
	private void toElaborateStatsCsv(String name) throws IOException {
		File csvFile = new File(Collection.PATH_CSV_RESULTS + name + "/endpointStats.csv");
		
		CSVWriter writer = new CSVWriter(new FileWriter(csvFile), ';');
		writer.writeNext(new String[]{name});
		
		writer.writeNext(new String[]{"Endpoint", "#queries", "accessibilityStatus", "triplesUsed"});
		
		Map<String, Integer> endpoints = getOrderedEndpoints();
		for (Entry<String, Integer> entry : endpoints.entrySet()) {
			String endpoint = entry.getKey();
			writeElaborareStatsRow(writer, endpoint, entry.getValue());
		}
		
		writer.close();
	}
	
	private void writeElaborareStatsRow(CSVWriter writer, String endpoint, int count) {
		String triplesNeeded = (endpointTriplesUsed != null && endpointTriplesUsed.containsKey(endpoint)? endpointTriplesUsed.get(endpoint).toString(): "");
		writer.writeNext(new String[]{endpoint, Integer.toString(count), accessibilityStats.getAccessibleStatus(endpoint).toString(), triplesNeeded});
	}
	
	private void toSimpleStatsCsv(String name) throws IOException {
		File csvFile = new File(Collection.PATH_CSV_RESULTS + name + "/endpointStatsSimple.csv");
		CSVWriter writer = new CSVWriter(new FileWriter(csvFile), ';');
		writer.writeNext(new String[]{name});
		writer.writeNext(new String[]{"Analysis", "Distinct endpoints", "Overall"});
		
		writeSimpleStatsRow(writer, "Number of endpoints", endpoints.size(), getTotalCount());
		writeSimpleStatsRow(writer, "Number of (>1 query) endpoints", getEndpoints(2).size(), getTotalCount(2));
		
		
		writeSimpleStatsRow(writer, "Number of accessible ckan endpoints", accessibilityStats.getNumEndpoints(EndpointAccessiblityStatus.CKAN_ACCESSIBLE), accessibilityStats.getTotalNumEndpoints(EndpointAccessiblityStatus.CKAN_ACCESSIBLE));
		writeSimpleStatsRow(writer, "Number of accessible endpoints not in ckan", accessibilityStats.getNumEndpoints(EndpointAccessiblityStatus.NOT_CKAN_BUT_ACCESSIBLE), accessibilityStats.getTotalNumEndpoints(EndpointAccessiblityStatus.NOT_CKAN_BUT_ACCESSIBLE));
		writeSimpleStatsRow(writer, "Number of probably incorrect endpoints", accessibilityStats.getNumEndpoints(EndpointAccessiblityStatus.PROBABLY_INCORRECT), accessibilityStats.getTotalNumEndpoints(EndpointAccessiblityStatus.PROBABLY_INCORRECT));
		writeSimpleStatsRow(writer, "Number of private endpoints with public data", accessibilityStats.getNumEndpoints(EndpointAccessiblityStatus.PRIVATE_ENDPOINT_PUBLIC_DATA), accessibilityStats.getTotalNumEndpoints(EndpointAccessiblityStatus.PRIVATE_ENDPOINT_PUBLIC_DATA));
		writeSimpleStatsRow(writer, "Number of private endpoints with private data", accessibilityStats.getNumEndpoints(EndpointAccessiblityStatus.PRIVATE_ENDPOINT_PRIVATE_DATA), accessibilityStats.getTotalNumEndpoints(EndpointAccessiblityStatus.PRIVATE_ENDPOINT_PRIVATE_DATA));
		writer.close();
	}
	
	private void writeSimpleStatsRow(CSVWriter writer, String analysis, int distinctEndpoints, int overallCount) {
		writer.writeNext(new String[]{analysis, Integer.toString(distinctEndpoints), Integer.toString(overallCount)});
	}
	public void runCoverageAnalysis(Collection collection) throws IOException {
		System.out.println("running query coverage analysis (is expensive)");
		endpointTriplesUsed = new HashMap<String,Integer>();
		int endpointCount = 0;
		int totalEndpointCount = getEndpoints().size();
		for (String endpoint: getEndpoints().keySet()) {
			endpointCount++;
			System.out.println("endpoint: " + endpointCount + "/" + totalEndpointCount);
			EndpointAccessiblityStatus status = collection.getEndpointCollection().getAccessibilityStats().getAccessibleStatus(endpoint);
			if (status != EndpointAccessiblityStatus.CKAN_ACCESSIBLE && status != EndpointAccessiblityStatus.NOT_CKAN_BUT_ACCESSIBLE) {
				continue;
			}
			
			int totalQueryCount = collection.getQueryCollection().getQueries(endpoint).size();
			int queryCount = 0;
			Set<String> triplesUsedInEndpoint = new HashSet<String>();
			for (Query query: collection.getQueryCollection().getQueries(endpoint)) {
				queryCount++;
				System.out.println("query: " + queryCount + "/" + totalQueryCount);
				triplesUsedInEndpoint.addAll(query.getUsedTriplesFromConstruct(endpoint));
			}
			endpointTriplesUsed.put(endpoint, triplesUsedInEndpoint.size());
		}
	}
}
