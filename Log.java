import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Log {
	public static final String BLACKHOLE = "sbl.spamhaus.org";

	public static boolean isLocalIP(String ip) {
		try {
			InetAddress inet = InetAddress.getByName(ip);
			return inet.isSiteLocalAddress();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean isSpam(String ip) {
		try {
			InetAddress address = InetAddress.getByName(ip);
			byte[] quad = address.getAddress();
			String query = BLACKHOLE;
			for (byte octet : quad) {
				int unsignedByte = octet < 0 ? octet + 256 : octet;
				query = unsignedByte + "." + query;
			}
			InetAddress.getByName(query);
			return true;
		} catch (UnknownHostException e) {
			return false;
		}
	}

	public static void main(String[] args) {
		ArrayList<String> spamIPs = new ArrayList<String>();
		Map<String, Integer> ipCount = new HashMap<>();

		String folderPath = "Log files";
		File folder = new File(folderPath);

		if (folder.exists() && folder.isDirectory()) {
			File[] files = folder.listFiles();
			for (File file : files) {
				if (file.isFile()) {
					System.out.println(file.getName());
					try (FileInputStream fin = new FileInputStream(file);
							Reader in = new InputStreamReader(fin);
							BufferedReader bin = new BufferedReader(in);) {
						for (String entry = bin.readLine(); entry != null; entry = bin.readLine()) {
							int index = entry.indexOf(' ');
							String ip = entry.substring(0, index);
							if (!isLocalIP(ip)) {
								if (isSpam(ip)) {
									spamIPs.add(ip);
								}
							}

							int count = ipCount.getOrDefault(ip, 0);
							ipCount.put(ip, count + 1);
							String theRest = entry.substring(index);

							try {
								InetAddress address = InetAddress.getByName(ip);
								System.out.println(address.getHostName() + theRest);
							} catch (UnknownHostException ex) {
								System.err.println(entry);
							}
						}
					} catch (IOException ex) {
						System.out.println("Exception: " + ex);
					}
				}
			}
		} else {
			System.out.println("Error: Folder does not exist!");
		}
		
		String filePath = "spam.txt";
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
			for (String line : spamIPs) {
				bw.write(line);
				bw.newLine();
			}
			System.out.println("Spam IPs written to file successfully!");
		} catch (IOException e) {
			e.printStackTrace();
		}

		Map<String, Integer> sortedIpCount = ipCount.entrySet().stream()
				.sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		System.out.println("Number of access of each IP:");
		for (Map.Entry<String, Integer> entry : sortedIpCount.entrySet()) {
			System.out.println(entry.getKey() + ": " + entry.getValue());
		}
	}
}
