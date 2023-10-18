package org.recap.camel.dailyreconciliation;

import com.amazonaws.services.s3.AmazonS3;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.spi.RouteController;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbConstants;
import org.recap.model.csv.DailyReconcilationRecord;
import org.recap.model.jpa.*;
import org.recap.repository.jpa.ItemDetailsRepository;
import org.recap.repository.jpa.RequestItemDetailsRepository;
import org.recap.util.PropertyUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.ExpectedCount.times;

/**
 * Created by akulak on 8/5/17.
 */

@RunWith(MockitoJUnitRunner.Silent.class)
public class DailyReconciliationProcessorUT{

    @InjectMocks
    DailyReconciliationProcessor dailyReconciliationProcessor;

    @Mock
    RequestItemDetailsRepository requestItemDetailsRepository;

    @Mock
    CamelContext camelContext;

    @Mock
    ProducerTemplate producerTemplate;

    @Mock
    Exchange exchange;

    @Mock
    Message message;

    @Mock
    RouteController routeController;

    @Mock
    ItemDetailsRepository itemDetailsRepository;

    @Mock
    AmazonS3 awsS3Client;

    @Mock
    PropertyUtil propertyUtil;


    @Value("${" + PropertyKeyConstants.DAILY_RECONCILIATION_FILE + "}")
    private String filePath;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.openMocks(this);
    }
    


    @Test
    public void processInput() throws Exception {
        ReflectionTestUtils.setField(dailyReconciliationProcessor,"imsLocationCode","RECAP");
        Mockito.when(exchange.getIn()).thenReturn(message);
        Mockito.when(propertyUtil.getPropertyByImsLocationAndKey("RECAP",PropertyKeyConstants.IMS.IMS_AVAILABLE_ITEM_STATUS_CODES)).thenReturn("RECAP");
        Mockito.when(propertyUtil.getPropertyByImsLocationAndKey("RECAP",PropertyKeyConstants.IMS.IMS_NOT_AVAILABLE_ITEM_STATUS_CODES)).thenReturn("RECAP");
        Mockito.when(propertyUtil.getPropertyByImsLocationAndKey("RECAP",PropertyKeyConstants.IMS.IMS_REQUESTABLE_NOT_RETRIEVABLE_ITEM_STATUS_CODES)).thenReturn("RECAP");
        List<DailyReconcilationRecord> dailyReconcilationRecords=new ArrayList<>();
        dailyReconcilationRecords.add(getDailyReconcilationRecord("12345","1", ScsbConstants.GFA_STATUS_IN));
        dailyReconcilationRecords.add(getDailyReconcilationRecord("2345","1", ScsbConstants.GFA_STATUS_IN ));
        Mockito.when(message.getBody()).thenReturn(dailyReconcilationRecords);
        Mockito.when(message.getHeader(Mockito.anyString())).thenReturn("CamelAwsS3Key/CamelAwsS3Key/CamelAwsS3Key");
        Mockito.when(camelContext.getRouteController()).thenReturn(routeController);
        Mockito.when(requestItemDetailsRepository.findById(Mockito.anyInt())).thenReturn(Optional.ofNullable(saveRequestItemEntity(1, getItemEntity())));
        Mockito.when(awsS3Client.doesObjectExist(Mockito.anyString(),Mockito.anyString())).thenReturn(true);
        Mockito.when(awsS3Client.doesBucketExistV2(Mockito.anyString())).thenReturn(true);
        ReflectionTestUtils.setField(dailyReconciliationProcessor,"filePath",filePath);
        dailyReconciliationProcessor.processInput(exchange);
    }
    @Test
    public void processInputRequestIdNull() throws Exception {
        Mockito.when(exchange.getIn()).thenReturn(message);
        List<DailyReconcilationRecord> dailyReconcilationRecords=new ArrayList<>();
        dailyReconcilationRecords.add(getDailyReconcilationRecord("12345",null, ScsbConstants.GFA_STATUS_IN));
        dailyReconcilationRecords.add(getDailyReconcilationRecord("2345",null, ScsbConstants.GFA_STATUS_IN ));
        List<ItemEntity> itemEntityList=new ArrayList<>();
        itemEntityList.add(getItemEntity());
        Mockito.when(itemDetailsRepository.findByBarcode(Mockito.anyString())).thenReturn(itemEntityList);
        Mockito.when(message.getBody()).thenReturn(dailyReconcilationRecords);
        Mockito.when(message.getHeader(Mockito.anyString())).thenReturn("CamelAwsS3Key/CamelAwsS3Key/CamelAwsS3Key");
        Mockito.when(camelContext.getRouteController()).thenReturn(routeController);
        Mockito.when(requestItemDetailsRepository.findById(Mockito.anyInt())).thenReturn(Optional.ofNullable(saveRequestItemEntity(1, getItemEntity())));
        Mockito.when(awsS3Client.doesObjectExist(Mockito.anyString(),Mockito.anyString())).thenReturn(true);
        Mockito.when(awsS3Client.doesBucketExistV2(Mockito.anyString())).thenReturn(true);
        ReflectionTestUtils.setField(dailyReconciliationProcessor,"filePath",filePath);
        dailyReconciliationProcessor.processInput(exchange);
    }


    @Test
    public void processInputRequestId() throws Exception {
        Mockito.when(exchange.getIn()).thenReturn(message);
        List<DailyReconcilationRecord> dailyReconcilationRecords=new ArrayList<>();
        dailyReconcilationRecords.add(getDailyReconcilationRecord("12345",null, ScsbConstants.GFA_STATUS_SCH_ON_REFILE_WORK_ORDER));
        dailyReconcilationRecords.add(getDailyReconcilationRecord("23451",null, ScsbConstants.GFA_STATUS_SCH_ON_REFILE_WORK_ORDER));
        Mockito.when(message.getBody()).thenReturn(dailyReconcilationRecords);
        Mockito.when(camelContext.getRouteController()).thenReturn(routeController);
        Mockito.when(itemDetailsRepository.findByBarcode(Mockito.anyString())).thenReturn(Arrays.asList(getItemEntity()));
        dailyReconciliationProcessor.processInput(exchange);
    }


    @Test
    public void processInputException() throws Exception {
        Mockito.when(exchange.getIn()).thenReturn(message);
        List<DailyReconcilationRecord> dailyReconcilationRecords=new ArrayList<>();
        DailyReconcilationRecord dailyReconcilationRecord = getDailyReconcilationRecord("12345","1","IN");
        dailyReconcilationRecords.add(dailyReconcilationRecord);
        Mockito.when(message.getBody()).thenReturn(dailyReconcilationRecords);
        Mockito.when(camelContext.getRouteController()).thenThrow(NullPointerException.class);
        dailyReconciliationProcessor.processInput(exchange);
    }

    private DailyReconcilationRecord getDailyReconcilationRecord(String barcode,String requestId,String status) {
        DailyReconcilationRecord dailyReconcilationRecord=new DailyReconcilationRecord();
        dailyReconcilationRecord.setCustomerCode("PA");
        dailyReconcilationRecord.setRequestId(requestId);
        dailyReconcilationRecord.setBarcode(barcode);
        dailyReconcilationRecord.setStopCode("PA");
        dailyReconcilationRecord.setPatronId("2");
        dailyReconcilationRecord.setCreateDate(new Date().toString());
        dailyReconcilationRecord.setOwningInst("1");
        dailyReconcilationRecord.setLastUpdatedDate(new Date().toString());
        dailyReconcilationRecord.setRequestingInst("1");
        dailyReconcilationRecord.setDeliveryMethod("test");
        dailyReconcilationRecord.setStatus(status);
        dailyReconcilationRecord.setErrorCode("");
        dailyReconcilationRecord.setErrorNote("");
        return dailyReconcilationRecord;
    }

    private ItemEntity getItemEntity() {
        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setId(new Random().nextInt());
        itemEntity.setBarcode("b3");
        itemEntity.setCustomerCode("c1");
        itemEntity.setCallNumber("cn1");
        itemEntity.setCallNumberType("ct1");
        itemEntity.setItemAvailabilityStatusId(1);
        itemEntity.setCopyNumber(1);
        itemEntity.setOwningInstitutionId(1);
        itemEntity.setCollectionGroupId(1);
        itemEntity.setCreatedDate(new Date());
        itemEntity.setCreatedBy("ut");
        itemEntity.setLastUpdatedDate(new Date());
        itemEntity.setLastUpdatedBy("ut");
        itemEntity.setUseRestrictions("no");
        itemEntity.setVolumePartYear("v3");
        itemEntity.setOwningInstitutionItemId(String.valueOf(new Random().nextInt()));
        itemEntity.setItemStatusEntity(getItemStatusEntity());
        itemEntity.setInstitutionEntity(getInstitutionEntity());
        itemEntity.setDeleted(false);
        return itemEntity;
    }

    public RequestItemEntity saveRequestItemEntity(Integer itemId,ItemEntity itemEntity){
        RequestItemEntity requestItemEntity = new RequestItemEntity();
        requestItemEntity.setItemId(itemId);
        requestItemEntity.setId(new Random().nextInt());
        requestItemEntity.setRequestTypeId(1);
        requestItemEntity.setCreatedBy("test");
        requestItemEntity.setStopCode("PA");
        requestItemEntity.setPatronId("45678912");
        requestItemEntity.setCreatedDate(new Date());
        requestItemEntity.setLastUpdatedDate(new Date());
        requestItemEntity.setEmailId("test@mail");
        requestItemEntity.setRequestStatusId(1);
        requestItemEntity.setRequestingInstitutionId(1);
        requestItemEntity.setInstitutionEntity(getInstitutionEntity());
        requestItemEntity.setRequestTypeEntity(getRequestTypeEntity());
        requestItemEntity.setItemEntity(itemEntity);
        return requestItemEntity;
    }

    private RequestTypeEntity getRequestTypeEntity() {
        RequestTypeEntity requestTypeEntity = new RequestTypeEntity();
        requestTypeEntity.setId(1);
        requestTypeEntity.setRequestTypeCode("EDD");
        requestTypeEntity.setRequestTypeDesc("EDD");
        return requestTypeEntity;
    }

    private ItemStatusEntity getItemStatusEntity() {
        ItemStatusEntity itemStatusEntity = new ItemStatusEntity();
        itemStatusEntity.setId(1);
        itemStatusEntity.setStatusCode("Available");
        itemStatusEntity.setStatusDescription("Available");
        return itemStatusEntity;
    }

    private InstitutionEntity getInstitutionEntity() {
        InstitutionEntity institutionEntity = new InstitutionEntity();
        institutionEntity.setId(1);
        institutionEntity.setInstitutionCode("PUL");
        institutionEntity.setInstitutionName("PUL");
        return institutionEntity;
    }

    @Test
    public void createHeaderForCompareSheetTest(){
        try {
            XSSFSheet xssfSheet = mock(XSSFSheet.class);
            XSSFRow row = mock(XSSFRow.class);
            Mockito.when(xssfSheet.createRow(0)).thenReturn(row);

            row.createCell(0).setCellValue(ScsbConstants.DAILY_RR_LAS);
            Mockito.when(row.createCell(0)).thenThrow(new RuntimeException("Exception occured"));
            Mockito.verify(xssfSheet).createRow(0);
            Mockito.verify(row.getCell(0)).setCellValue(ScsbConstants.DAILY_RR_LAS);
            ReflectionTestUtils.invokeMethod(dailyReconciliationProcessor, "createHeaderForCompareSheet", xssfSheet);
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    private void verifyCellValue(XSSFSheet sheet, int rowNumber, int cellNumber, String expectedValue) {
        Row row = sheet.getRow(rowNumber);
        Cell cell = row.getCell(cellNumber);
        assertEquals(expectedValue, cell.getStringCellValue());
    }

}
