package model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;

public class HttpHelper {
    static private HashMap<String, Tuple<Double, Date>> cache = new HashMap<>();

    public static double convertCurrency(double amount, String from, String to) throws Exception{
        Date validityDate = new Date(System.currentTimeMillis() - (15 * 60 * 1000));

        if (cache.containsKey(from+to)) {
            if (cache.get(from + to).y.after(validityDate)) {
                return cache.get(from + to).x * amount;
            } else{
                cache.remove(from+to);
            }
        }

        String url = "https://xe.com/currencyconverter/convert/?Amount=1&From=" + from + "&To=" + to;

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

            if (!response.toString().toUpperCase().contains("'"+from.toUpperCase()+"'") || !response.toString().toUpperCase().contains("'"+to.toUpperCase
                    ()+"'")){
                throw new NumberFormatException();
            }

            int index = response.indexOf("<span class='uccResultAmount'>") + "<span class='uccResultAmount'>".length();
            double myRes = Double.parseDouble(response.substring(index, index+7));
            double rate = Math.round(myRes*100)/100.;

            cache.put(from + to, new Tuple<>(rate, new Date(System.currentTimeMillis())));

            System.out.println(from + " to " + to + " rate = " + rate);

            if (rate < 0.001){
                throw new IOException("exchange rate is less than 0.001!!!");
            }

            return rate * amount;
        } catch (IOException e) {
            throw new IOException("can't get exchange rate from xe.com");
        } catch (NumberFormatException e){
            throw new NumberFormatException("probably xc.com doesn't have exchange rate for this currency");
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
