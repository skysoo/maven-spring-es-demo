package com.elastic.service;

import com.elastic.aop.LogExecutionTime;
import com.elastic.configuration.HUCConfiguration;
import com.elastic.configuration.EsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * @author skysoo
 * @version 1.0.0
 * @since 2019-12-19 오후 3:09
 **/

@Slf4j
@RestController
@RequestMapping("/es")
public class HUCController {
    private String noiseData = null;
    private final EsProperties esProperties;
    private final HUCConfiguration HUCConfiguration;
    private final NoiseDataManager noiseDataManager;

    public HUCController(HUCConfiguration HUCConfiguration, EsProperties esProperties, NoiseDataManager noiseDataManager) {
        this.HUCConfiguration = HUCConfiguration;
        this.esProperties = esProperties;
        this.noiseDataManager = noiseDataManager;
    }

    private String urlStr(){
        return esProperties.getProtocol()+"://"+ esProperties.getHost()+":"+ esProperties.getPort()+"/";
    }

    @LogExecutionTime
    @GetMapping("/get_cluster_stat")
    public void esClusterStat(@RequestParam String indexName,@RequestParam String method) {
        if(HUCConfiguration.get(urlStr()+"_cluster/stats",method))
            log.info("##### cluster stat is good ");
    }

    @LogExecutionTime
    @GetMapping("/get_index_doc_count")
    public void esIndexDocCount(@RequestParam String indexName,@RequestParam String method) {
        if(HUCConfiguration.get(urlStr()+indexName+"/_count",method))
            log.info("##### {} get the data count ",indexName);
    }

    @LogExecutionTime
    @GetMapping("/get_doc_search")
    public void esDocSearch(@RequestParam String indexName,@RequestParam String method){
       if(HUCConfiguration.get(urlStr()+indexName+"/_search?size=1",method))
           log.info("##### {} search the data ",indexName);
    }

    @LogExecutionTime
    @GetMapping("/get_and_put_noise_data")
    public void noiseDataFromLocal(@RequestParam String fileName,@RequestParam String indexName){
        noiseData = noiseDataManager.getNoiseData(fileName);
        if (noiseData == null)
            log.warn("##### Noise Data Download Failed / fileName : {}",fileName);
        log.info("##### Success the Noise Data Download from Local {}",fileName);
//        esIndexSave(indexName);
    }

    @LogExecutionTime
    @PostMapping("/post_noise_data")
    public void esIndexSave(@RequestParam String indexName,@RequestParam String fileName){
        noiseData = noiseDataManager.getNoiseData("D:\\99.TEMP\\noise\\"+fileName);
        if (noiseData == null){
            log.warn("##### Noise Data Download Failed / fileName : {}",fileName);
        } else{
            log.debug("##### Noise data length is {}",noiseData.length());
            log.info("##### Success the Noise Data Download from Local {}",fileName);
        }

        String stringJson = "{\n" +
                "    \"noise_data\" : \""+noiseData+"\" \n" +
                "}";

        if(HUCConfiguration.post(urlStr()+indexName+"/_doc?pretty",stringJson)) {
            log.info("##### Success the data save in es # {}",indexName);
        } else {
            log.warn("##### Failed save the data in es # {}",indexName);
        }
    }

    @LogExecutionTime
    @PutMapping("/put_create_index")
    public void esIndexCreate(@RequestParam String indexName,@RequestParam int shardNum,@RequestParam int replicaNum) {
        if(HUCConfiguration.put(urlStr()+ indexName + "?pretty", "{\n" +
                "    \"settings\" : {\n" +
                "        \"index\" : {\n" +
                "            \"number_of_shards\" : "+shardNum+", \n" +
                "            \"number_of_replicas\" : "+replicaNum+" \n" +
                "        },\n" +
                "\t\t\"index.codec\": \"best_compression\"\n" +
                "    }\n" +
                "}"))
            log.info("##### "+indexName+" is created.");
    }

    @LogExecutionTime
    @DeleteMapping("/delete_index")
    public void esDeleteIndex(@RequestParam String indexName){
        if(HUCConfiguration.delete(urlStr()+indexName))
            log.info("##### "+indexName+" is deleted.");
    }
}
