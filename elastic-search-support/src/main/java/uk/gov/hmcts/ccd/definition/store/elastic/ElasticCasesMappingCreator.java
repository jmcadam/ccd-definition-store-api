package uk.gov.hmcts.ccd.definition.store.elastic;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.definition.store.repository.entity.CaseTypeEntity;


@Service
@Slf4j
public class ElasticCasesMappingCreator extends AbstractElasticSearchSupport {

    @Autowired
    private CaseMappingGenerator mappingGenerator;

    public void createMapping(CaseTypeEntity caseType) throws IOException {
        String caseMapping = mappingGenerator.generate(caseType);
        PutMappingRequest request = createPutMappingRequest(indexName(caseType), caseMapping);
        PutMappingResponse putMappingResponse = elasticClient.indices().putMapping(request);
        boolean acknowledged = putMappingResponse.isAcknowledged();

        log.info("mapping created: {}", acknowledged);
    }

    private PutMappingRequest createPutMappingRequest(String indexName, String mappings) {
        PutMappingRequest request = new PutMappingRequest(indexName);
        request.type(config.getIndexCasesType());
        request.source(mappings, XContentType.JSON);
        return request;
    }
}
