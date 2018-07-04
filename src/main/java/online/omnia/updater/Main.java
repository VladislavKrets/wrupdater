package online.omnia.updater;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by lollipop on 08.02.2018.
 */
public class Main {
    public static void main(String[] args) {
        try (BufferedReader fileReader = new BufferedReader(new FileReader("configuration.ini"))) {
            String[] parameters;
            String line;
            File file;
            BufferedReader campaignReader;
            HttpURLConnection httpcon;
            BufferedReader reader;
            OutputStream os;
            List<SourceStatisticsEntity> sourceStatisticsEntities;
            FileWriter writer;
            StringBuilder lineBuilder;
            byte[] outputBytes;
            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(List.class, new StatisticsJsonDeserializer());
            Gson gson = builder.create();
            String answer;
            Date currentDate = new Date();
            SimpleDateFormat fileDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String apiKey = "13d674a58cf43573bff3e59fa7db9d02e42b9a8f13d674a58cf43573bff3e59f";
            String str;
            double maxBid;
            double maxWR;
            double maxCTR;
            String id;
            while (fileReader.ready()) {
                line = fileReader.readLine();
                System.out.println(line);
                if (line.matches("[\\w\\.]+ [\\w\\.]+ [\\w\\.]+ [\\w\\.]+ [\\w\\.]+")) {
                    parameters = line.split(" ");
                    id = parameters[0];
                    maxBid = Double.parseDouble(parameters[2]);
                    maxWR = Double.parseDouble(parameters[3]);
                    apiKey = parameters[4];
                    str = "{\"groupings\":[" +
                            "\"campaign_campaign_id\"," +
                            "\"date_date\"," +
                            "\"campaign_name\"," +
                            "\"placement_id\"" +
                            "]," +
                            "\"columns\":[" +
                            "\"click\"," +
                            "\"ctr\"," +
                            "\"conversion\"," +
                            "\"cr\"," +
                            "\"cpm\"," +
                            "\"cpc\"," +
                            "\"cpa\"," +
                            "\"cost\"," +
                            "\"revenue\"," +
                            "\"profit\"," +
                            "\"bid\"," +
                            "\"win\"," +
                            "\"wr\"" +
                            "]," +
                            "\"filters\":[" +
                            "]," +
                            "\"start_date\":\"" + simpleDateFormat.format(currentDate) + "\"," +
                            "\"end_date\":\"" + simpleDateFormat.format(currentDate) + "\"" +
                            "}";
                    System.out.println(str);
                    httpcon = (HttpURLConnection) ((new URL("https://api.go2mobi.com/v1/reports").openConnection()));
                    httpcon.setDoOutput(true);
                    httpcon.setRequestProperty("Content-Type", "application/json");
                    httpcon.setRequestProperty("Authorization", "Token " + apiKey);
                    httpcon.setRequestMethod("POST");
                    httpcon.connect();
                    outputBytes = str.getBytes("UTF-8");
                    os = httpcon.getOutputStream();
                    os.write(outputBytes);
                    os.close();
                    reader = new BufferedReader(new InputStreamReader(httpcon.getInputStream()));
                    lineBuilder = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        lineBuilder.append(line);
                    }
                    answer = HttpMethodUtils.getMethod(httpcon.getHeaderField("Location") + "?page_number=1&page_size=500", apiKey);
                    System.out.println(answer);
                    sourceStatisticsEntities = gson.fromJson(answer, List.class);

                    for (SourceStatisticsEntity sourceStatisticsEntity : sourceStatisticsEntities) {
                        if (sourceStatisticsEntity.getCampaignId().equals(id)) {
                            file = new File(sourceStatisticsEntity.getCampaignId()
                                    + "_" + sourceStatisticsEntity.getPlacementId() + ".txt");
                            if (!file.exists()) {
                                file.createNewFile();
                            }
                            line = null;
                            campaignReader = new BufferedReader(new FileReader(file));
                            while (campaignReader.ready()) {
                                line = campaignReader.readLine();
                            }
                            writer = new FileWriter(sourceStatisticsEntity.getCampaignId()
                                    + "_" + sourceStatisticsEntity.getPlacementId() + ".txt", true);

                            writer.write(fileDateFormat.format(new Date())
                                    + " " + Math.round((double) (sourceStatisticsEntity.getWin() / sourceStatisticsEntity.getWr() * 100))
                                    + " " + sourceStatisticsEntity.getWin()
                                    + " " + sourceStatisticsEntity.getCtr() + " " + sourceStatisticsEntity.getBid() + "\n");
                            writer.flush();
                            writer.close();

                            if (line != null) {
                                parameters = line.split(" ");
                                double wr = ((double) (sourceStatisticsEntity.getWin() - Integer.parseInt(parameters[3])))
                                        / (Math.round((double) (sourceStatisticsEntity.getWin()
                                        / sourceStatisticsEntity.getWr() * 100)) - Integer.parseInt(parameters[2])) * 100;
                                if (wr < maxWR && sourceStatisticsEntity.getBid() < maxBid) {
                                    str = "{\"bid_cpm\" : " + (sourceStatisticsEntity.getCpm() + 0.01) + "," +
                                            " \"placement_id\" : " + sourceStatisticsEntity.getPlacementId() + "}";
                                    httpcon = (HttpURLConnection) ((new URL("https://api.go2mobi.com/v1/campaigns/"
                                            + sourceStatisticsEntity.getCampaignId() + "/overrides").openConnection()));
                                    httpcon.setDoOutput(true);
                                    httpcon.setRequestProperty("Content-Type", "application/json");
                                    httpcon.setRequestProperty("Authorization", "Token " + apiKey);
                                    httpcon.setRequestMethod("POST");
                                    httpcon.connect();
                                    outputBytes = str.getBytes("UTF-8");
                                    os = httpcon.getOutputStream();
                                    os.write(outputBytes);
                                    os.close();
                                    reader = new BufferedReader(new InputStreamReader(httpcon.getInputStream()));
                                    lineBuilder = new StringBuilder();
                                    while ((line = reader.readLine()) != null) {
                                        lineBuilder.append(line);
                                    }
                                }
                                if (sourceStatisticsEntity.getBid() >= maxBid) continue;
                                if (wr > maxWR) {
                                    str = "{\"bid_cpm\" : " + (sourceStatisticsEntity.getCpm() - 0.02) + "," +
                                            " \"placement_id\" : " + sourceStatisticsEntity.getPlacementId() + "}";
                                    httpcon = (HttpURLConnection) ((new URL("https://api.go2mobi.com/v1/campaigns/"
                                            + sourceStatisticsEntity.getCampaignId() + "/overrides").openConnection()));
                                    httpcon.setDoOutput(true);
                                    httpcon.setRequestProperty("Content-Type", "application/json");
                                    httpcon.setRequestProperty("Authorization", "Token " + apiKey);
                                    httpcon.setRequestMethod("POST");
                                    httpcon.connect();
                                    outputBytes = str.getBytes("UTF-8");
                                    os = httpcon.getOutputStream();
                                    os.write(outputBytes);
                                    os.close();
                                    reader = new BufferedReader(new InputStreamReader(httpcon.getInputStream()));
                                    lineBuilder = new StringBuilder();
                                    while ((line = reader.readLine()) != null) {
                                        lineBuilder.append(line);
                                    }
                                }
                            }
                            campaignReader.close();
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

