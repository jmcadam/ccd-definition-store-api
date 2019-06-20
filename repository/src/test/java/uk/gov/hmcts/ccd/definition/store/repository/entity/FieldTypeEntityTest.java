package uk.gov.hmcts.ccd.definition.store.repository.entity;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static uk.gov.hmcts.ccd.definition.store.repository.FieldTypeUtils.BASE_COLLECTION;
import static uk.gov.hmcts.ccd.definition.store.repository.FieldTypeUtils.BASE_COMPLEX;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.hamcrest.core.Is.is;

class FieldTypeEntityTest {

    private ComplexFieldEntity field1 = new ComplexFieldEntity();
    private ComplexFieldEntity field2 = new ComplexFieldEntity();
    private FieldTypeEntity classUnderTest;

    @BeforeEach
    void setup() {
        classUnderTest = new FieldTypeEntity();
    }

    @Test
    @DisplayName("should return empty list for no base field type")
    void getChildrenForNoBaseType() {
        final List<ComplexFieldEntity> children = classUnderTest.getChildren();
            assertThat(children.size(), is(0));
    }

    @Test
    @DisplayName("should return empty list if base type is not Complex or Collection")
    void getChildrenForNonComplexOrCollectiomnBaseTypes() {
        final List<ComplexFieldEntity> children = classUnderTest.getChildren();
            assertThat(children.size(), is(0));
    }

    @Test
    @DisplayName("should get children for complex field type")
    void getChildrenForComplex() {
        FieldTypeEntity complex = new FieldTypeEntity();
        complex.setReference(BASE_COMPLEX);
        classUnderTest.setBaseFieldType(complex);
        classUnderTest.addComplexFields(asList(field1, field2));

        final List<ComplexFieldEntity> children = classUnderTest.getChildren();
        assertAll(
            () -> assertThat(children.size(), is(2)),
            () -> assertThat(children.get(0), is(field1)),
            () -> assertThat(children.get(1), is(field2))
        );
    }

    @Test
    @DisplayName("should get children for collection field type")
    void getChildrenForCollection() {
        FieldTypeEntity collectionBaseType = new FieldTypeEntity();
        collectionBaseType.setReference(BASE_COLLECTION);
        FieldTypeEntity collectionFieldType = new FieldTypeEntity();
        collectionFieldType.addComplexFields(asList(field1, field2));
        classUnderTest.setBaseFieldType(collectionBaseType);
        classUnderTest.setCollectionFieldType(collectionFieldType);

        final List<ComplexFieldEntity> children = classUnderTest.getChildren();

        assertAll(
            () -> assertThat(children.size(), is(2)),
            () -> assertThat(children.get(0), is(field1)),
            () -> assertThat(children.get(1), is(field2))
        );
    }
}