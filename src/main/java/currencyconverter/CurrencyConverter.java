package currencyconverter;

import currencyconverter.exceptions.CouldNotResolveValueException;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CurrencyConverter {
    public static void main(String[] args) {
        final String urlExchangeInfo = "https://www.x-rates.com/table/?from=USD&amount=1";

        try {
            final Document document = Jsoup.connect(urlExchangeInfo).get();

            HashMap<String, Float> currencyAndCorrespondingDollarValue = getCurrencyAndCorrespondentDollarValue(document);

            HashMap<String, String> currencyLongNameToISO4217 = getCurrencyLongNameToISO4217(currencyAndCorrespondingDollarValue);

            // get user input
            // convert currency 1 to dollar
            // convert dollar to currency 2
            // output
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static HashMap<String, String> getCurrencyLongNameToISO4217(HashMap<String, Float> currencyAndCorrespondingDollarValue) throws IOException, CouldNotResolveValueException {
        HashMap<String, String> currencyLongNameToISO4217 = new HashMap<>();

        for (String currencyLongName : currencyAndCorrespondingDollarValue.keySet()) {
            try {
                String urlSearchCurrentCurrency = buildGoogleSearchUrl(currencyLongName);
                Document wikiResult = Jsoup.connect(urlSearchCurrentCurrency).get();
                for (Element element : wikiResult.getAllElements()) {
                    if (element.text().contains("wikipedia")) {
                        try {
                            currencyLongNameToISO4217.put(currencyLongName, getISO4217CurrencyCode(element));
                            break;
                        } catch (CouldNotResolveValueException e) {
                            throw new CouldNotResolveValueException("Could not resolve the ISO 4217 for " + currencyLongName);
                        }
                    }
                }
            } catch (HttpStatusException e) {
                e.printStackTrace();
            }
        }

        return currencyLongNameToISO4217;
    }

    private static HashMap<String, Float> getCurrencyAndCorrespondentDollarValue(Document document) {
        HashMap<String, Float> currencyAndCorrespondingDollarValue = new HashMap<>();
        Element table = document.select("table").get(1);

        for (Element row : table.select("tr")) {
            if (row.select("td").size() > 0) {
                List<Element> currentRow = row.select("td");
                currencyAndCorrespondingDollarValue.put(
                        currentRow.get(0).text(),
                        Float.valueOf(currentRow.get(1).text())
                );
            }
        }

        return currencyAndCorrespondingDollarValue;
    }

    private static String buildGoogleSearchUrl(String currencyLongName) {
        final String urlGoogleSearch = "https://www.google.com/search?q=";
        final String endOfSearchQuery = "+currency+code";

        return urlGoogleSearch + currencyLongName.replace(" ", "+") + endOfSearchQuery;
    }

    private static String getISO4217CurrencyCode(Element element) throws CouldNotResolveValueException {
        List<Element> emResults = element.select("em");
        Pattern isoPattern = Pattern.compile("[A-Z]{3}");
        int i = 0;

        while (i < emResults.size()) {
            String currentEm = emResults.get(i++).text();
            Matcher match = isoPattern.matcher(currentEm);
            if (match.find()) {
                int j = 0;
                while (j < match.groupCount() + 1) {
                    if (!Objects.equals(match.group(j), "ISO")) return match.group(j);

                    j++;
                }
            }
        }

        throw new CouldNotResolveValueException("Could not resolve the ISO 4217");
    }
}
