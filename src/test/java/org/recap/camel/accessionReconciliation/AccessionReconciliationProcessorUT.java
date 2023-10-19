package org.recap.camel.accessionReconciliation;

import com.amazonaws.services.s3.AmazonS3;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.RouteController;
import org.apache.camel.support.DefaultExchange;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.recap.BaseTestCaseUT;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.camel.accessionreconciliation.AccessionReconciliationProcessor;
import org.recap.camel.accessionreconciliation.BarcodeReconcilitaionReport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.event.annotation.AfterTestMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.apache.camel.builder.Builder.simple;

@RunWith(MockitoJUnitRunner.Silent.class)
public class AccessionReconciliationProcessorUT  {
    @InjectMocks
    AccessionReconciliationProcessor mockedAccessionReconciliationProcessor;

    @Mock
    Exchange exchange;

    @Mock
    Message message;

    @Mock
    CamelContext camelContext;

    @Mock
    RouteController routeController;

    @Mock
    RestTemplate restTemplate;

    @Mock
    AmazonS3 awsS3Client;

    @Value("${" + PropertyKeyConstants.SCSB_SOLR_DOC_URL + "}")
    String solrSolrClientUrl;

    @Value("${" + PropertyKeyConstants.ACCESSION_RECONCILIATION_FILEPATH + "}")
    String accessionFilePath;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @AfterTestMethod
    public void finall() throws IOException {
        ReflectionTestUtils.setField(mockedAccessionReconciliationProcessor, "solrSolrClientUrl", solrSolrClientUrl);
        ReflectionTestUtils.setField(mockedAccessionReconciliationProcessor, "accessionFilePath", accessionFilePath);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(ScsbConstants.BARCODE_RECONCILIATION_FILE_DATE_FORMAT);
        Path filePathPul = Paths.get(accessionFilePath+ ScsbCommonConstants.PATH_SEPARATOR+"pul"+ScsbCommonConstants.PATH_SEPARATOR+ ScsbConstants.ACCESSION_RECONCILATION_FILE_NAME+"pul"+simpleDateFormat.format(new Date())+".csv");
        Path filePathCul = Paths.get(accessionFilePath+ ScsbCommonConstants.PATH_SEPARATOR+"cul"+ScsbCommonConstants.PATH_SEPARATOR+ ScsbConstants.ACCESSION_RECONCILATION_FILE_NAME+"cul"+simpleDateFormat.format(new Date())+".csv");
        Path filePathNypl = Paths.get(accessionFilePath+ ScsbCommonConstants.PATH_SEPARATOR+"nypl"+ScsbCommonConstants.PATH_SEPARATOR+ ScsbConstants.ACCESSION_RECONCILATION_FILE_NAME+"nypl"+simpleDateFormat.format(new Date())+".csv");
        boolean deletedPul = Files.deleteIfExists(filePathPul);
        Assert.assertTrue(deletedPul);
        boolean deletedCul = Files.deleteIfExists(filePathCul);
        Assert.assertTrue(deletedCul);
        boolean deletedNypl = Files.deleteIfExists(filePathNypl);
        Assert.assertTrue(deletedNypl);

    }
    @Test
    public void processInput() throws Exception {
        ReflectionTestUtils.setField(mockedAccessionReconciliationProcessor, "solrSolrClientUrl", solrSolrClientUrl);
        ReflectionTestUtils.setField(mockedAccessionReconciliationProcessor, "accessionFilePath", accessionFilePath);
        BarcodeReconcilitaionReport barcodeReconcilitaionReport = getBarcodeReconcilitaionReport();
        ArrayList<BarcodeReconcilitaionReport> barcodeReconcilitaionReports = new ArrayList<>();
        barcodeReconcilitaionReports.add(0,barcodeReconcilitaionReport);
        CamelContext ctx = new DefaultCamelContext();
        Exchange ex = getExchange(barcodeReconcilitaionReports, ctx);
        ex.setProperty(ScsbConstants.CAMEL_SPLIT_INDEX,1);
        Map<String, Boolean> map = new HashMap<>();
        map.put("accessionReconcilationService", true);
        ReflectionTestUtils.setField(mockedAccessionReconciliationProcessor, "institutionCode", "pul");
        ReflectionTestUtils.setField(mockedAccessionReconciliationProcessor, "camelContext", ctx);
        ResponseEntity<Map> responseEntity = new ResponseEntity<Map>(map, HttpStatus.OK);
        HashMap<String,String> barcodesAndOwnerCodes=new HashMap<>();
        barcodesAndOwnerCodes.put(barcodeReconcilitaionReport.getBarcode(),barcodeReconcilitaionReport.getCustomerCode());
        HttpEntity httpEntity = new HttpEntity(barcodesAndOwnerCodes);
        Mockito.when(restTemplate.exchange(solrSolrClientUrl+ ScsbConstants.ACCESSION_RECONCILATION_SOLR_CLIENT_URL, HttpMethod.POST, httpEntity,Map.class)).thenReturn(responseEntity);
        Mockito.when(camelContext.getRouteController()).thenReturn(routeController);
        mockedAccessionReconciliationProcessor.processInput(ex);
    }

    @Test
    public void processInputCamelException() throws Exception {
        ReflectionTestUtils.setField(mockedAccessionReconciliationProcessor, "solrSolrClientUrl", solrSolrClientUrl);
        ReflectionTestUtils.setField(mockedAccessionReconciliationProcessor, "accessionFilePath", accessionFilePath);
        BarcodeReconcilitaionReport barcodeReconcilitaionReport = getBarcodeReconcilitaionReport();
        ArrayList<BarcodeReconcilitaionReport> barcodeReconcilitaionReports = new ArrayList<>();
        barcodeReconcilitaionReports.add(0,barcodeReconcilitaionReport);
        CamelContext ctx = new DefaultCamelContext();
        Exchange ex = getExchange(barcodeReconcilitaionReports, ctx);
        ex.setProperty(ScsbConstants.CAMEL_SPLIT_INDEX,1);
        Map<String, Boolean> map = new HashMap<>();
        map.put("accessionReconcilationService", true);
        ReflectionTestUtils.setField(mockedAccessionReconciliationProcessor, "institutionCode", "pul");
        ReflectionTestUtils.setField(mockedAccessionReconciliationProcessor, "camelContext", camelContext);
        ResponseEntity<Map> responseEntity = new ResponseEntity<Map>(map, HttpStatus.OK);
        HashMap<String,String> barcodesAndOwnerCodes=new HashMap<>();
        barcodesAndOwnerCodes.put(barcodeReconcilitaionReport.getBarcode(),barcodeReconcilitaionReport.getCustomerCode());
        HttpEntity httpEntity = new HttpEntity(barcodesAndOwnerCodes);
        Mockito.when(restTemplate.exchange(solrSolrClientUrl+ ScsbConstants.ACCESSION_RECONCILATION_SOLR_CLIENT_URL, HttpMethod.POST, httpEntity,Map.class)).thenReturn(responseEntity);
        Mockito.when(camelContext.getRouteController()).thenReturn(routeController);
        Mockito.doThrow(NullPointerException.class).when(routeController).startRoute(Mockito.anyString());
        mockedAccessionReconciliationProcessor.processInput(ex);
    }

    @Test
    public void processInputNoFilePath() throws Exception {
        Random random=new Random();
        String accessionFilePath=String.valueOf(random.nextInt());
        ReflectionTestUtils.setField(mockedAccessionReconciliationProcessor, "solrSolrClientUrl", solrSolrClientUrl);
        ReflectionTestUtils.setField(mockedAccessionReconciliationProcessor, "accessionFilePath", accessionFilePath);
        BarcodeReconcilitaionReport barcodeReconcilitaionReport = getBarcodeReconcilitaionReport();
        ArrayList<BarcodeReconcilitaionReport> barcodeReconcilitaionReports = new ArrayList<>();
        barcodeReconcilitaionReports.add(0,barcodeReconcilitaionReport);
        CamelContext ctx = new DefaultCamelContext();
        Exchange ex = getExchange(barcodeReconcilitaionReports, ctx);
        ex.setProperty(ScsbConstants.CAMEL_SPLIT_INDEX,1);
        Map<String, Boolean> map = new HashMap<>();
        map.put("accessionReconcilationService", true);
        ReflectionTestUtils.setField(mockedAccessionReconciliationProcessor, "institutionCode", "pul");
        ReflectionTestUtils.setField(mockedAccessionReconciliationProcessor, "camelContext", ctx);
        ResponseEntity<Map> responseEntity = new ResponseEntity<Map>(map, HttpStatus.OK);
        HashMap<String,String> barcodesAndOwnerCodes=new HashMap<>();
        barcodesAndOwnerCodes.put(barcodeReconcilitaionReport.getBarcode(),barcodeReconcilitaionReport.getCustomerCode());
        HttpEntity httpEntity = new HttpEntity(barcodesAndOwnerCodes);
        Mockito.when(restTemplate.exchange(solrSolrClientUrl+ ScsbConstants.ACCESSION_RECONCILATION_SOLR_CLIENT_URL, HttpMethod.POST, httpEntity,Map.class)).thenReturn(responseEntity);
        Mockito.when(camelContext.getRouteController()).thenReturn(routeController);
        mockedAccessionReconciliationProcessor.processInput(ex);
    }

    @Test
    public void processInputException() throws Exception {
        ReflectionTestUtils.setField(mockedAccessionReconciliationProcessor, "solrSolrClientUrl", solrSolrClientUrl);
        ReflectionTestUtils.setField(mockedAccessionReconciliationProcessor, "accessionFilePath", new String());
        BarcodeReconcilitaionReport barcodeReconcilitaionReport = getBarcodeReconcilitaionReport();
        ArrayList<BarcodeReconcilitaionReport> barcodeReconcilitaionReports = new ArrayList<>();
        barcodeReconcilitaionReports.add(0,barcodeReconcilitaionReport);
        CamelContext ctx = new DefaultCamelContext();
        Exchange ex = getExchange(barcodeReconcilitaionReports, ctx);
        ex.setProperty(ScsbConstants.CAMEL_SPLIT_INDEX,1);
        Map<String, Boolean> map = new HashMap<>();
        map.put("accessionReconcilationService", true);
        ReflectionTestUtils.setField(mockedAccessionReconciliationProcessor, "institutionCode", "pul");
        ReflectionTestUtils.setField(mockedAccessionReconciliationProcessor, "camelContext", ctx);
        ResponseEntity<Map> responseEntity = new ResponseEntity<Map>(map, HttpStatus.OK);
        HashMap<String,String> barcodesAndOwnerCodes=new HashMap<>();
        barcodesAndOwnerCodes.put(barcodeReconcilitaionReport.getBarcode(),barcodeReconcilitaionReport.getCustomerCode());
        HttpEntity httpEntity = new HttpEntity(barcodesAndOwnerCodes);
        Mockito.when(restTemplate.exchange(solrSolrClientUrl+ ScsbConstants.ACCESSION_RECONCILATION_SOLR_CLIENT_URL, HttpMethod.POST, httpEntity,Map.class)).thenReturn(responseEntity);
        Mockito.when(camelContext.getRouteController()).thenReturn(routeController);
        mockedAccessionReconciliationProcessor.processInput(ex);
    }

    @Test
    public void processInputWithoutIndex() throws Exception {
        ReflectionTestUtils.setField(mockedAccessionReconciliationProcessor, "solrSolrClientUrl", solrSolrClientUrl);
        ReflectionTestUtils.setField(mockedAccessionReconciliationProcessor, "accessionFilePath", accessionFilePath);
        BarcodeReconcilitaionReport barcodeReconcilitaionReport = getBarcodeReconcilitaionReport();
        ArrayList<BarcodeReconcilitaionReport> barcodeReconcilitaionReports = new ArrayList<>();
        barcodeReconcilitaionReports.add(0,barcodeReconcilitaionReport);
        CamelContext ctx = new DefaultCamelContext();
        Exchange ex = getExchange(barcodeReconcilitaionReports, ctx);
        ex.setProperty(ScsbConstants.CAMEL_SPLIT_INDEX,0);
        Map<String, Boolean> map = new HashMap<>();
        map.put("accessionReconcilationService", true);
        ReflectionTestUtils.setField(mockedAccessionReconciliationProcessor, "institutionCode", "cul");
        ResponseEntity<Map> responseEntity = new ResponseEntity<Map>(map, HttpStatus.OK);
        HashMap<String,String> barcodesAndOwnerCodes=new HashMap<>();
        barcodesAndOwnerCodes.put(barcodeReconcilitaionReport.getBarcode(),barcodeReconcilitaionReport.getCustomerCode());
        HttpEntity httpEntity = new HttpEntity(barcodesAndOwnerCodes);
        Mockito.when(restTemplate.exchange(solrSolrClientUrl+ ScsbConstants.ACCESSION_RECONCILATION_SOLR_CLIENT_URL, HttpMethod.POST, httpEntity,Map.class)).thenReturn(responseEntity);
        Mockito.when(camelContext.getRouteController()).thenReturn(routeController);
        Mockito.when(awsS3Client.doesObjectExist(Mockito.anyString(),Mockito.anyString())).thenReturn(true);
        Mockito.when(awsS3Client.doesBucketExistV2(Mockito.anyString())).thenReturn(true);
        ReflectionTestUtils.setField(mockedAccessionReconciliationProcessor, "camelContext", ctx);
        ReflectionTestUtils.setField(mockedAccessionReconciliationProcessor, "institutionCode", "nypl");
        mockedAccessionReconciliationProcessor.processInput(ex);
    }

    private Exchange getExchange(ArrayList<BarcodeReconcilitaionReport> barcodeReconcilitaionReports, CamelContext ctx) {
        Exchange ex = new DefaultExchange(ctx);
        Message in = ex.getIn();
        ex.setMessage(in);
        ex.setIn(in);

        ex.setProperty(ScsbConstants.CAMEL_SPLIT_COMPLETE,true);
        in.setHeader("CamelAwsS3Key", simple("CamelAwsS3Key/CamelAwsS3Key/CamelAwsS3Key"));
        in.setHeader("CamelAwsS3BucketName", simple("CamelAwsS3BucketName"));
        in.setBody(barcodeReconcilitaionReports);
        return ex;
    }

    private BarcodeReconcilitaionReport getBarcodeReconcilitaionReport() {
        BarcodeReconcilitaionReport barcodeReconcilitaionReport = new BarcodeReconcilitaionReport();
        barcodeReconcilitaionReport.setBarcode("123456");
        barcodeReconcilitaionReport.setCustomerCode("PA");
        barcodeReconcilitaionReport.setStatus("SUCCESS");
        return barcodeReconcilitaionReport;
    }
}
