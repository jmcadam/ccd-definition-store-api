package uk.gov.hmcts.ccd.definition.store.excel.parser;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.definition.store.excel.endpoint.exception.MapperException;
import uk.gov.hmcts.ccd.definition.store.excel.parser.model.DefinitionSheet;

@DisplayName("SearchResultLayoutParser Tests")
public class SearchResultLayoutParserTest {

    @Mock
    private ParseContext parseContext;

    private SearchResultLayoutParser classUnderTest;
    private Map<String, DefinitionSheet> definitionSheets;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);

        definitionSheets = new HashMap<>();
        classUnderTest = new SearchResultLayoutParser(parseContext, new EntityToDefinitionDataItemRegistry());
    }

    @Test
    @DisplayName("Should Fail when no worksheet provided")
    public void shouldThrowExceptionWhenWorkbasketInputWorksheetIsNotProvided() {
        MapperException thrown = assertThrows(MapperException.class, () -> classUnderTest.getDefinitionSheet(definitionSheets));
        assertEquals("A definition must contain a SearchResultFields sheet", thrown.getMessage());
    }

    @Test
    @DisplayName("Should return name")
    public void shouldReturnNameWhenAsked() {
        assertThat(classUnderTest.getLayoutName(), is("Search Results"));
    }
}
