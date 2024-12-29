package zerobase.weather.service;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import zerobase.weather.WeatherApplication;
import zerobase.weather.domain.DateWeather;
import zerobase.weather.domain.Diary;
import zerobase.weather.error.InvalidDate;
import zerobase.weather.repository.DateWeatherRepository;
import zerobase.weather.repository.DiaryRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DiaryService {

    @Value("${openweathermap.key}")
    private String apiKey;

    private final DiaryRepository diaryRepository;
    private final DateWeatherRepository dateWeatherRepository;


    private static final Logger logger = LoggerFactory.getLogger(WeatherApplication.class);

    public DiaryService(DiaryRepository diaryRepository, DateWeatherRepository dateWeatherRepository) {
        this.diaryRepository = diaryRepository;
        this.dateWeatherRepository = dateWeatherRepository;
    }



    @Transactional
    @Scheduled(cron = "0 0 1 * * *")
    public void saveWeatherDate(){

        dateWeatherRepository.save(getWeatherFromApi());
    }


    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void createDiary(LocalDate date, String text) {
        /* 매일 새벽 1시에 날씨데이터를 가져오기때문에 글을 쓸 때마다 날씨데이터를 가져올 필요가 없다.
        // open weather map에서 날씨 데이터 가져오기
        String weatherData = getWeatherString();
        // 받아온 날씨 json 파싱하기
        Map<String, Object> parseWeather = parseWeather(weatherData);
        */


        //logger.info("started to create diary");
        // 날씨 데이터 가져오기 (API 에서 가져오기? or DB에서 가져오기)
        DateWeather dateWeather = getDateWeather(date);

        // 파싱된 데이터 + 일기값 우리 DB에 넣기
        Diary nowDiary = new Diary();
        nowDiary.setDateWeather(dateWeather);
        nowDiary.setText(text);
        nowDiary.setDate(date);
        diaryRepository.save(nowDiary);
        //logger.info("finished to create diary");
        /*
        nowDiary.setWeather(parseWeather.get("main").toString());
        nowDiary.setIcon(parseWeather.get("icon").toString());
        nowDiary.setTemperature((Double)parseWeather.get("temp"));
         */
    }

    private DateWeather getWeatherFromApi(){

        // open weather map에서 날씨 데이터 가져오기
        String weatherData = getWeatherString();
        // 받아온 날씨 json 파싱하기
        Map<String, Object> parseWeather = parseWeather(weatherData);
        DateWeather dateWeather = new DateWeather();
        dateWeather.setDate(LocalDate.now());
        dateWeather.setWeather(parseWeather.get("main").toString());
        dateWeather.setIcon(parseWeather.get("icon").toString());
        dateWeather.setTemperature((Double)parseWeather.get("temp"));

        return dateWeather;
    }

    private DateWeather getDateWeather(LocalDate date){
        List<DateWeather> dateWeatherListFromDB = dateWeatherRepository.findAllByDate(date);
        if(dateWeatherListFromDB.size() == 0){
            // 새로 api에서 날씨 정보를 가져와야한다.
            // 정책상... 현재 날씨를 가져오도록 하거나.. 날씨없이 일기를 쓰도록...
            return getWeatherFromApi();
        }else{
            return dateWeatherListFromDB.get(0);
        }
    }
    @Transactional(readOnly = true)
    public List<Diary> readDiary(LocalDate date){
        /*logger.debug("read diary");

        if(date.isAfter(LocalDate.ofYearDay(3050, 1))){
            throw new InvalidDate();
        }
         */
        return diaryRepository.findAllByDate(date);
    }

    public List<Diary> readDiaries(LocalDate startDate, LocalDate endDate){


        return diaryRepository.findAllByDateBetween(startDate, endDate);
    }

    public void updateDiary(LocalDate date, String text){
        Diary noewDiary = diaryRepository.getFirstByDate(date);
        noewDiary.setText(text);
        diaryRepository.save(noewDiary);

    }

    public void deleteDiary(LocalDate date){
        diaryRepository.deleteAllByDate(date);
    }

    private String getWeatherString() {
        String apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=seoul&appid=" + apiKey;

        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            BufferedReader br;
            if(responseCode == 200) {
                br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            }else{
                br = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            }
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }

            br.close();
            return response.toString();
        } catch (Exception e) {
            return "failed to get response";
        }
    }

    private Map<String, Object> parseWeather(String jsonString){
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject;


        try{
            jsonObject = (JSONObject) jsonParser.parse(jsonString);
        }catch (ParseException e){
            throw new RuntimeException(e);
        }

        Map<String, Object> resultMap = new HashMap<>();

        JSONObject mainData = (JSONObject) jsonObject.get("main");
        resultMap.put("temp",mainData.get("temp"));
        JSONArray weatherArray = (JSONArray) jsonObject.get("weather");
        JSONObject weatherDate = (JSONObject) weatherArray.get(0);
        resultMap.put("main", weatherDate.get("main"));
        resultMap.put("icon",weatherDate.get("icon"));

        return resultMap;

    }
}
