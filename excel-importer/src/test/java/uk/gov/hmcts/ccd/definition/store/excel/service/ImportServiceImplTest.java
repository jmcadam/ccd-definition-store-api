package uk.gov.hmcts.ccd.definition.store.excel.service;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationEventPublisher;
import uk.gov.hmcts.ccd.definition.store.domain.service.FieldTypeService;
import uk.gov.hmcts.ccd.definition.store.domain.service.JurisdictionService;
import uk.gov.hmcts.ccd.definition.store.domain.service.LayoutService;
import uk.gov.hmcts.ccd.definition.store.domain.service.casetype.CaseTypeService;
import uk.gov.hmcts.ccd.definition.store.domain.service.metadata.MetadataField;
import uk.gov.hmcts.ccd.definition.store.domain.service.workbasket.WorkBasketUserDefaultService;
import uk.gov.hmcts.ccd.definition.store.domain.showcondition.ShowConditionParser;
import uk.gov.hmcts.ccd.definition.store.event.DefinitionImportedEvent;
import uk.gov.hmcts.ccd.definition.store.excel.domain.definition.model.DefinitionFileUploadMetadata;
import uk.gov.hmcts.ccd.definition.store.excel.endpoint.exception.InvalidImportException;
import uk.gov.hmcts.ccd.definition.store.excel.parser.EntityToDefinitionDataItemRegistry;
import uk.gov.hmcts.ccd.definition.store.excel.parser.MetadataCaseFieldEntityFactory;
import uk.gov.hmcts.ccd.definition.store.excel.parser.ParseContext;
import uk.gov.hmcts.ccd.definition.store.excel.parser.ParserFactory;
import uk.gov.hmcts.ccd.definition.store.excel.parser.SpreadsheetParser;
import uk.gov.hmcts.ccd.definition.store.excel.validation.SpreadsheetValidator;
import uk.gov.hmcts.ccd.definition.store.repository.CaseFieldRepository;
import uk.gov.hmcts.ccd.definition.store.repository.UserRoleRepository;
import uk.gov.hmcts.ccd.definition.store.repository.entity.CaseFieldEntity;
import uk.gov.hmcts.ccd.definition.store.repository.entity.CaseTypeEntity;
import uk.gov.hmcts.ccd.definition.store.repository.entity.DataFieldType;
import uk.gov.hmcts.ccd.definition.store.repository.entity.FieldTypeEntity;
import uk.gov.hmcts.ccd.definition.store.repository.entity.JurisdictionEntity;
import uk.gov.hmcts.ccd.definition.store.rest.model.IdamProperties;
import uk.gov.hmcts.ccd.definition.store.rest.service.IdamProfileClient;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.definition.store.repository.FieldTypeUtils.*;

@RunWith(MockitoJUnitRunner.class)
public class ImportServiceImplTest {

    public static final String BAD_FILE = "CCD_TestDefinition_V12.xlsx";
    private static final String GOOD_FILE = "CCD_TestDefinition.xlsx";
    private static final String JURISDICTION_NAME = "TEST";
    private static final String TEST_ADDRESS_BOOK_CASE_TYPE = "TestAddressBookCase";
    private static final String TEST_COMPLEX_ADDRESS_BOOK_CASE_TYPE = "TestComplexAddressBookCase";

    private ImportServiceImpl service;

    @Mock
    private FieldTypeService fieldTypeService;

    @Mock
    private SpreadsheetValidator spreadsheetValidator;

    @Mock
    private JurisdictionService jurisdictionService;

    @Mock
    private CaseTypeService caseTypeService;

    @Mock
    private LayoutService layoutService;

    @Mock
    private JurisdictionEntity jurisdiction;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private WorkBasketUserDefaultService workBasketUserDefaultService;

    @Mock
    private CaseFieldRepository caseFieldRepository;

    @Mock
    private MetadataCaseFieldEntityFactory metadataCaseFieldEntityFactory;

    @Mock
    private IdamProfileClient idamProfileClient;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Captor
    private ArgumentCaptor<DefinitionImportedEvent> eventCaptor;

    private FieldTypeEntity fixedTypeBaseType;
    private FieldTypeEntity multiSelectBaseType;
    private FieldTypeEntity complexType;
    private FieldTypeEntity textBaseType;
    private FieldTypeEntity numberBaseType;
    private FieldTypeEntity emailBaseType;
    private FieldTypeEntity yesNoBaseType;
    private FieldTypeEntity dateBaseType;
    private FieldTypeEntity dateTimeBaseType;
    private FieldTypeEntity postCodeBaseType;
    private FieldTypeEntity moneyGBPBaseType;
    private FieldTypeEntity phoneUKBaseType;
    private FieldTypeEntity textAreaBaseType;
    private FieldTypeEntity collectionBaseType;
    private FieldTypeEntity documentBaseType;
    private FieldTypeEntity labelBaseType;
    private FieldTypeEntity casePaymentHistoryViewerBaseType;
    private FieldTypeEntity caseHistoryViewerBaseType;
    private FieldTypeEntity fixedListRadioTypeBaseType;
    private FieldTypeEntity dynamicListBaseType;

    @Before
    public void setup() {
        Map<MetadataField, MetadataCaseFieldEntityFactory> registry = new HashMap<>();
        registry.put(MetadataField.STATE, metadataCaseFieldEntityFactory);

        final ParserFactory parserFactory = new ParserFactory(new ShowConditionParser(),
            new EntityToDefinitionDataItemRegistry(), registry, spreadsheetValidator);

        final SpreadsheetParser spreadsheetParser = new SpreadsheetParser(spreadsheetValidator);

        service = new ImportServiceImpl(spreadsheetValidator,
                                        spreadsheetParser,
                                        parserFactory,
                                        fieldTypeService,
                                        jurisdictionService,
                                        caseTypeService,
                                        layoutService,
                                        userRoleRepository,
                                        workBasketUserDefaultService,
                                        caseFieldRepository,
                                        applicationEventPublisher,
                                        idamProfileClient);

        fixedTypeBaseType = buildBaseType(BASE_FIXED_LIST);
        dynamicListBaseType = buildBaseType(BASE_DYNAMIC_LIST);
        multiSelectBaseType = buildBaseType(BASE_MULTI_SELECT_LIST);
        complexType = buildBaseType(BASE_COMPLEX);
        textBaseType = buildBaseType(BASE_TEXT);
        numberBaseType = buildBaseType(BASE_NUMBER);
        emailBaseType = buildBaseType(BASE_EMAIL);
        yesNoBaseType = buildBaseType(BASE_YES_OR_NO);
        dateBaseType = buildBaseType(BASE_DATE);
        dateTimeBaseType = buildBaseType(BASE_DATE_TIME);
        postCodeBaseType = buildBaseType(BASE_POST_CODE);
        moneyGBPBaseType = buildBaseType(BASE_MONEY_GBP);
        phoneUKBaseType = buildBaseType(BASE_PHONE_UK);
        textAreaBaseType = buildBaseType(BASE_TEXT_AREA);
        collectionBaseType = buildBaseType(BASE_COLLECTION);
        documentBaseType = buildBaseType(BASE_DOCUMENT);
        labelBaseType = buildBaseType(BASE_LABEL);
        casePaymentHistoryViewerBaseType = buildBaseType(BASE_CASE_PAYMENT_HISTORY_VIEWER);
        caseHistoryViewerBaseType = buildBaseType(BASE_CASE_HISTORY_VIEWER);
        fixedListRadioTypeBaseType = buildBaseType(BASE_RADIO_FIXED_LIST);

        given(jurisdiction.getReference()).willReturn(JURISDICTION_NAME);

        final IdamProperties idamProperties = new IdamProperties();
        idamProperties.setId("445");
        idamProperties.setEmail("user@hmcts.net");

        doReturn(idamProperties).when(idamProfileClient).getLoggedInUserDetails();
    }

    @Test(expected = InvalidImportException.class)
    public void shouldNotImportDefinition() throws Exception {

        given(jurisdictionService.get(JURISDICTION_NAME)).willReturn(Optional.of(jurisdiction));
        given(fieldTypeService.getBaseTypes()).willReturn(Arrays.asList(fixedTypeBaseType,
            multiSelectBaseType,
            complexType));
        given(fieldTypeService.getTypesByJurisdiction(JURISDICTION_NAME)).willReturn(Lists.newArrayList());

        final InputStream inputStream = getClass().getClassLoader().getResourceAsStream(BAD_FILE);

        service.importFormDefinitions(inputStream);
    }

    @Test
    public void shouldImportDefinition() throws Exception {

        given(jurisdictionService.get(JURISDICTION_NAME)).willReturn(Optional.of(jurisdiction));
        given(fieldTypeService.getBaseTypes()).willReturn(Arrays.asList(fixedTypeBaseType,
            multiSelectBaseType,
            complexType,
            textBaseType,
            numberBaseType,
            emailBaseType,
            yesNoBaseType,
            dateBaseType,
            dateTimeBaseType,
            postCodeBaseType,
            moneyGBPBaseType,
            phoneUKBaseType,
            textAreaBaseType,
            collectionBaseType,
            documentBaseType,
            labelBaseType,
            casePaymentHistoryViewerBaseType,
            caseHistoryViewerBaseType,
            fixedListRadioTypeBaseType,
            dynamicListBaseType));
        given(fieldTypeService.getTypesByJurisdiction(JURISDICTION_NAME)).willReturn(Lists.newArrayList());
        CaseFieldEntity caseRef = new CaseFieldEntity();
        caseRef.setReference("[CASE_REFERENCE]");
        given(caseFieldRepository.findByDataFieldTypeAndCaseTypeNull(DataFieldType.METADATA))
            .willReturn(Collections.singletonList(caseRef));
        CaseFieldEntity state = new CaseFieldEntity();
        state.setReference("[STATE]");
        state.setDataFieldType(DataFieldType.METADATA);
        given(metadataCaseFieldEntityFactory.createCaseFieldEntity(any(ParseContext.class), any(CaseTypeEntity.class)))
            .willReturn(state);

        final InputStream inputStream = getClass().getClassLoader().getResourceAsStream(GOOD_FILE);

        final DefinitionFileUploadMetadata metadata = service.importFormDefinitions(inputStream);
        assertEquals(JURISDICTION_NAME, metadata.getJurisdiction());
        assertEquals(2, metadata.getCaseTypes().size());
        assertEquals(TEST_ADDRESS_BOOK_CASE_TYPE, metadata.getCaseTypes().get(0));
        assertEquals(TEST_COMPLEX_ADDRESS_BOOK_CASE_TYPE, metadata.getCaseTypes().get(1));
        assertEquals("user@hmcts.net", metadata.getUserId());
        assertEquals(TEST_ADDRESS_BOOK_CASE_TYPE + "," + TEST_COMPLEX_ADDRESS_BOOK_CASE_TYPE,
            metadata.getCaseTypesAsString());

        verify(caseFieldRepository).findByDataFieldTypeAndCaseTypeNull(DataFieldType.METADATA);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getContent().size(), equalTo(2));
    }

    @Test
    public void shouldReturnImportWarnings() {
        Map<MetadataField, MetadataCaseFieldEntityFactory> registry = new HashMap<>();
        registry.put(MetadataField.STATE, metadataCaseFieldEntityFactory);

        final ParserFactory parserFactory = new ParserFactory(new ShowConditionParser(),
            new EntityToDefinitionDataItemRegistry(), registry, spreadsheetValidator);

        final SpreadsheetParser spreadsheetParser = mock(SpreadsheetParser.class);

        service = new ImportServiceImpl(spreadsheetValidator,
            spreadsheetParser,
            parserFactory,
            fieldTypeService,
            jurisdictionService,
            caseTypeService,
            layoutService,
            userRoleRepository,
            workBasketUserDefaultService,
            caseFieldRepository,
            applicationEventPublisher,
            idamProfileClient);

        final List<String> importWarnings = Arrays.asList("Warning1", "Warning2");

        given(spreadsheetParser.getImportWarnings()).willReturn(importWarnings);

        final List<String> warnings = service.getImportWarnings();
        assertThat(warnings.size(), equalTo(2));
        assertThat(importWarnings, containsInAnyOrder("Warning1", "Warning2"));
        verify(spreadsheetParser).getImportWarnings();
   }

    private FieldTypeEntity buildBaseType(final String reference) {
        FieldTypeEntity fieldTypeEntity = new FieldTypeEntity();
        fieldTypeEntity.setReference(reference);
        return fieldTypeEntity;
    }
}
