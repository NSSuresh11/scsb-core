package org.recap.service.accession;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.apache.camel.Exchange;
import org.apache.commons.collections.CollectionUtils;
import org.recap.RecapCommonConstants;
import org.recap.RecapConstants;
import org.recap.model.accession.AccessionRequest;
import org.recap.model.accession.AccessionResponse;
import org.recap.model.accession.AccessionSummary;
import org.recap.model.jpa.AccessionEntity;
import org.recap.model.jpa.ReportDataEntity;
import org.recap.repository.jpa.AccessionDetailsRepository;
import org.recap.service.accession.callable.BibDataCallable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by sheiks on 26/05/17.
 */
@Service
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class BulkAccessionService extends AccessionService{

    private static final Logger logger = LoggerFactory.getLogger(BulkAccessionService.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private AccessionValidationService accessionValidationService;

    @Autowired
    private AccessionDetailsRepository accessionDetailsRepository;

    /**
     * The batch accession thread size.
     */
    @Value("${batch.accession.thread.size}")
    int batchAccessionThreadSize;

    /**
     * This method saves the accession request in database and returns the status message.
     *
     * @param accessionRequestList
     * @return
     */
    @Transactional
    public String saveRequest(List<AccessionRequest> accessionRequestList) {
        List<AccessionRequest> trimmedAccessionRequests = getTrimmedAccessionRequests(accessionRequestList);
        String status;
        try {
            AccessionEntity accessionEntity = new AccessionEntity();
            accessionEntity.setAccessionRequest(convertJsonToString(trimmedAccessionRequests));
            accessionEntity.setCreatedDate(new Date());
            accessionEntity.setAccessionStatus(RecapConstants.PENDING);
            accessionDetailsRepository.save(accessionEntity);
            status = RecapConstants.ACCESSION_SAVE_SUCCESS_STATUS;
        } catch (Exception ex) {
            logger.error(RecapCommonConstants.LOG_ERROR, ex);
            status = RecapConstants.ACCESSION_SAVE_FAILURE_STATUS + RecapCommonConstants.EXCEPTION_MSG + " : " + ex.getMessage();
        }
        return status;
    }

    /**
     * This method is used to find the list of accession entity based on the accession status.
     *
     * @param accessionStatus
     * @return
     */
    public List<AccessionEntity> getAccessionEntities(String accessionStatus) {
        return accessionDetailsRepository.findByAccessionStatus(accessionStatus);
    }

    /**
     * This method is used to get the accession request for the given accession list.
     *
     * @param accessionEntityList
     * @return
     */
    public List<AccessionRequest> getAccessionRequest(List<AccessionEntity> accessionEntityList) {
        List<AccessionRequest> accessionRequestList = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(accessionEntityList)) {
            try {
                for(AccessionEntity accessionEntity : accessionEntityList) {
                    TypeReference<List<AccessionRequest>> typeReference = new TypeReference<>() {
                    };
                    accessionRequestList.addAll(new ObjectMapper().readValue(accessionEntity.getAccessionRequest(), typeReference));
                }
            } catch(Exception e) {
                logger.error(RecapCommonConstants.LOG_ERROR, e);
            }
        }
        return accessionRequestList;
    }


    public void updateStatusForAccessionEntities(List<AccessionEntity> accessionEntities, String status) {
        for(AccessionEntity accessionEntity : accessionEntities) {
            accessionEntity.setAccessionStatus(status);
        }
        accessionDetailsRepository.saveAll(accessionEntities);
    }

    @Override
    public List<AccessionResponse> doAccession(List<AccessionRequest> accessionRequestList, AccessionSummary accessionSummary, Exchange exhange) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        int requestedCount = accessionRequestList.size();
        List<AccessionRequest> trimmedAccessionRequests = getTrimmedAccessionRequests(accessionRequestList);
        trimmedAccessionRequests = accessionProcessService.removeDuplicateRecord(trimmedAccessionRequests);

        int duplicateCount = requestedCount - trimmedAccessionRequests.size();
        accessionSummary.setRequestedRecords(requestedCount);
        accessionSummary.setDuplicateRecords(duplicateCount);

        ExecutorService executorService = Executors.newFixedThreadPool(batchAccessionThreadSize);

        List<List<AccessionRequest>> partitions = Lists.partition(trimmedAccessionRequests, batchAccessionThreadSize);


            for (Iterator<List<AccessionRequest>> iterator = partitions.iterator(); iterator.hasNext(); ) {
                List<AccessionRequest> accessionRequests = iterator.next();
                List<Future> futures = new ArrayList<>();
                List<AccessionRequest> failedRequests = new ArrayList<>();
                for (Iterator<AccessionRequest> accessionRequestIterator = accessionRequests.iterator(); accessionRequestIterator.hasNext(); ) {
                    AccessionRequest accessionRequest = accessionRequestIterator.next();
                    logger.info("Processing accession for item barcode----->{}", accessionRequest.getItemBarcode());
                    // validate empty barcode ,customer code and owning institution
                    String itemBarcode = accessionRequest.getItemBarcode();
                    String customerCode = accessionRequest.getCustomerCode();
                    AccessionValidationService.AccessionValidationResponse accessionValidationResponse = accessionValidationService.validateBarcodeOrCustomerCode(itemBarcode, customerCode);

                    String owningInstitution = accessionValidationResponse.getOwningInstitution();

                    if (!accessionValidationResponse.isValid()) {
                        String message = accessionValidationResponse.getMessage();
                        List<ReportDataEntity> reportDataEntityList = new ArrayList<>(accessionUtil.createReportDataEntityList(accessionRequest, message));
                        accessionUtil.saveReportEntity(owningInstitution, reportDataEntityList);
                        addCountToSummary(accessionSummary, message);
                        continue;
                    }

                    BibDataCallable bibDataCallable = applicationContext.getBean(BibDataCallable.class);
                    bibDataCallable.setAccessionRequest(accessionRequest);
                    bibDataCallable.setOwningInstitution(owningInstitution);
                    futures.add(executorService.submit(bibDataCallable));

                }
                for (Iterator<Future> futureIterator = futures.iterator(); futureIterator.hasNext(); ) {
                    Future bibDataFuture = futureIterator.next();
                    try {
                        Object object = bibDataFuture.get();
                        if (object instanceof Set) {
                            prepareSummary(accessionSummary, object);
                        } else if (object instanceof AccessionRequest) {
                            failedRequests.add((AccessionRequest) object);
                        }

                    } catch (Exception e) {
                        logger.error(RecapCommonConstants.LOG_ERROR, e);
                        exhange.setException(e);
                    }
                }

                // Processed failed barcodes one by one
                for (Iterator<AccessionRequest> accessionRequestIterator = failedRequests.iterator(); accessionRequestIterator.hasNext(); ) {
                    AccessionRequest accessionRequest = accessionRequestIterator.next();
                    BibDataCallable bibDataCallable = applicationContext.getBean(BibDataCallable.class);
                    bibDataCallable.setAccessionRequest(accessionRequest);
                    bibDataCallable.setWriteToReport(true);
                    String owningInstitution = accessionUtil.getOwningInstitution(accessionRequest.getCustomerCode());
                    bibDataCallable.setOwningInstitution(owningInstitution);
                    Future submit = executorService.submit(bibDataCallable);
                    try {
                        Object o = submit.get();
                        prepareSummary(accessionSummary, o);
                    } catch (Exception e) {
                        logger.error(RecapCommonConstants.LOG_ERROR, e);
                        exhange.setException(e);
                        accessionSummary.addException(1);
                    }
                }
            }
            executorService.shutdown();
            stopWatch.stop();

        logger.info("Total time taken to accession for all barcode -> {} sec",stopWatch.getTotalTimeSeconds());
        return null;
    }


    /**
     * This method converts the json object to string.
     * @param objJson
     * @return
     */
    private String convertJsonToString(Object objJson) {
        String strJson = "";
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            strJson = objectMapper.writeValueAsString(objJson);
        } catch (JsonProcessingException ex) {
            logger.error(RecapCommonConstants.LOG_ERROR, ex);
        }
        return strJson;
    }

    /*@Override
    public BibliographicEntity saveBibRecord(BibliographicEntity fetchBibliographicEntity) {
        return accessionDAO.saveBibRecord(fetchBibliographicEntity);
    }*/

}