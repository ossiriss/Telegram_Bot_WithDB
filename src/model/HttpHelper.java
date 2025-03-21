package model;

import my_bots.TravelBot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HttpHelper {
    static private HashMap<String, Tuple<Double, Date>> cash = new HashMap<>();

    public static double convertCurrency(double amount, String from, String to) throws Exception{
        Date validityDate = new Date(System.currentTimeMillis() - (24 * 60 * 60 * 1000));

        if (cash.containsKey(from+to)) {
            if (cash.get(from + to).y.after(validityDate)) {
                return cash.get(from + to).x * amount;
            } else{
                cash.remove(from+to);
            }
        }

        String urlString = "https://v6.exchangerate-api.com/v6/" + MyConstants.ExchangerateApiKey + "/latest/" + from;

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        // Create an ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();

        // Parse the JSON string
        JsonNode jsonNode = objectMapper.readTree(response.toString());

        // Get the 'conversion_rates' node
        JsonNode conversionRatesNode = jsonNode.path("conversion_rates");

        // Iterate over the conversion rates
        Iterator<Map.Entry<String, JsonNode>> fields = conversionRatesNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String currencyCode = field.getKey();
            double conversionRate = field.getValue().asDouble();

            if (!currencyCode.equals(from) && conversionRate > 0.001){
                cash.put(from + currencyCode, new Tuple<>(conversionRate, new Date(System.currentTimeMillis())));
            }
        }

        if (cash.containsKey(from+to)) {
            return cash.get(from + to).x * amount;
        }else{
            throw new IOException("exchange from " + from + " to " + to + " not found on v6.exchangerate-api.com");
        }
    }

    public static double convertCurrencyOld(double amount, String from, String to) throws Exception{
        Date validityDate = new Date(System.currentTimeMillis() - (60 * 60 * 1000));

        if (cash.containsKey(from+to)) {
            if (cash.get(from + to).y.after(validityDate)) {
                return cash.get(from + to).x * amount;
            } else{
                cash.remove(from+to);
            }
        }

        String url = "http://free.currencyconverterapi.com/api/v5/convert?q="+ from +"_"+ to + "&compact=y&apiKey=" + TravelBot.getCurrencyToken();

        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            if (!response.toString().toUpperCase().contains(from.toUpperCase() + "_" + to.toUpperCase())){
                throw new NumberFormatException();
            }

            String pattern = "\\{\"val\":(\\d+\\.\\d+)\\}";

            // Create a Pattern object
            Pattern r = Pattern.compile(pattern);
            double myRes = 0;
            // Now create matcher object.
            Matcher m = r.matcher(response);
            if (m.find( )) {
                myRes = Double.parseDouble(m.group(1));
            }else {
                throw new NumberFormatException();
            }

            cash.put(from + to, new Tuple<>(myRes, new Date(System.currentTimeMillis())));

            System.out.println(from + " to " + to + " rate = " + myRes);

            if (myRes < 0.001){
                throw new IOException("exchange rate is less than 0.001!!!");
            }

            return myRes * amount;
        } catch (IOException e) {
            throw new IOException("can't get exchange rate from currencyconverterapi");
        } catch (NumberFormatException e){
            throw new NumberFormatException("probably currencyconverterapi doesn't have exchange rate for this currency");
        }

    }

    public static class Tuple<X, Y> {
        public final X x;
        public final Y y;
        public Tuple(X x, Y y) {
            this.x = x;
            this.y = y;
        }
    }
}
