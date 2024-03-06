package io.github.dumijdev.dpxml.parser;

import io.github.dumijdev.dpxml.model.*;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXParseException;

public class DefaultPojolizerTests {

    private static String xml;
    private static String clazzXml;
    private static String employeeXml;
    private static String companyXml;
    private static String body;
    private static Pojolizer pojolizer;

    @BeforeAll
    static void setup() {
        xml = "<root><name>Dumildes Paulo</name><age>77</age></root>";
        pojolizer = new DefaultPojolizer();
        clazzXml = "<root><person><name>Dumildes Paulo</name><age>77</age></person><person><name>Thiago Santana</name><age>77</age></person></root>";
        employeeXml = "<employee><id>5000</id><person><name>Dumildes Paulo</name><age>77</age></person></employee>";
        companyXml = "<root><id></id><employee><id>5000</id><person><name>Dumildes Paulo</name><age>77</age></person></employee></root>";
        body = "<soap:Body><ns7:createInstantTransferRequestResponse xmlns=\"http://schema.xcore.mw.pst.asseco.com/PaymentExecution\" xmlns:ns10=\"http://schema.xcore.mw.pst.asseco.com/CurrentAccountTransferency\" xmlns:ns11=\"http://schema.xcore.mw.pst.asseco.com/AccountHold\" xmlns:ns12=\"http://schema.xcore.mw.pst.asseco.com/PaginatedNoCountResultList\" xmlns:ns13=\"http://schema.xcore.mw.pst.asseco.com/CurrentAccountTransferencySTC\" xmlns:ns14=\"http://schema.xcore.mw.pst.asseco.com/DirectDebitMandate\" xmlns:ns15=\"http://schema.xcore.mw.pst.asseco.com/TermDeposit\" xmlns:ns16=\"http://schema.xcore.mw.pst.asseco.com/CurrentAccountTransferencySPTR\" xmlns:ns17=\"http://schema.xcore.mw.pst.asseco.com/SubmitFile\" xmlns:ns18=\"http://schema.xcore.mw.pst.asseco.com/SubmitPayment\" xmlns:ns19=\"http://schema.xcore.mw.pst.asseco.com/CreditCard\" xmlns:ns2=\"http://schema.xcore.mw.pst.asseco.com/fault\" xmlns:ns20=\"http://schema.xcore.mw.pst.asseco.com/Party\" xmlns:ns21=\"http://schema.xcore.mw.pst.asseco.com/PledgedAsset\" xmlns:ns22=\"http://schema.xcore.mw.pst.asseco.com/SecurityPledgedAsset\" xmlns:ns23=\"http://schema.xcore.mw.pst.asseco.com/ContractParty\" xmlns:ns24=\"http://schema.xcore.mw.pst.asseco.com/Contract\" xmlns:ns25=\"http://schema.xcore.mw.pst.asseco.com/Contact\" xmlns:ns26=\"http://schema.xcore.mw.pst.asseco.com/ProductDefinitionDetail\" xmlns:ns27=\"http://schema.xcore.mw.pst.asseco.com/PartyRelationship\" xmlns:ns28=\"http://schema.xcore.mw.pst.asseco.com/Account\" xmlns:ns29=\"http://schema.xcore.mw.pst.asseco.com/EstatePledgedAsset\" xmlns:ns3=\"http://schema.xcore.mw.pst.asseco.com/PaymentExecutionSTI\" xmlns:ns30=\"http://schema.xcore.mw.pst.asseco.com/CustomPledgedAsset\" xmlns:ns31=\"http://schema.xcore.mw.pst.asseco.com/LetterOfCreditPayment\" xmlns:ns32=\"http://schema.xcore.mw.pst.asseco.com/LetterOfCredit\" xmlns:ns33=\"http://schema.xcore.mw.pst.asseco.com/ApplicationPledgedAsset\" xmlns:ns34=\"http://schema.xcore.mw.pst.asseco.com/Collateral\" xmlns:ns35=\"http://schema.xcore.mw.pst.asseco.com/AdditionalInfo\" xmlns:ns36=\"http://schema.xcore.mw.pst.asseco.com/FinancialTransaction\" xmlns:ns37=\"http://schema.xcore.mw.pst.asseco.com/LedgerAccount\" xmlns:ns38=\"http://schema.xcore.mw.pst.asseco.com/GeneralLoanAccountNature\" xmlns:ns39=\"http://schema.xcore.mw.pst.asseco.com/LoanRequest\" xmlns:ns4=\"http://schema.xcore.mw.pst.asseco.com/AccountMovement\" xmlns:ns40=\"http://schema.xcore.mw.pst.asseco.com/CollateralIntervenient\" xmlns:ns41=\"http://schema.xcore.mw.pst.asseco.com/ServicePaymentEntity\" xmlns:ns42=\"http://wsdl.xcore.mw.pst.asseco.com/operationsNExecution/crossProductOperations/payments/PaymentExecutionRequest\" xmlns:ns5=\"http://schema.xcore.mw.pst.asseco.com/PaymentOrder\" xmlns:ns6=\"http://schema.xcore.mw.pst.asseco.com/CreateSupplierPayment\" xmlns:ns7=\"http://wsdl.xcore.mw.pst.asseco.com/operationsNExecution/crossProductOperations/payments/PaymentExecutionResponse\" xmlns:ns8=\"http://schema.xcore.mw.pst.asseco.com/ParameterizedPaymentExecutionStatus\" xmlns:ns9=\"http://schema.xcore.mw.pst.asseco.com/EMISPaymentExecution\"><ns6:operationNumber>611452760</ns6:operationNumber></ns7:createInstantTransferRequestResponse></soap:Body>";
    }

    @DisplayName("Should Pojolize simple string")
    @Test
    @SneakyThrows
    void shouldPojolizeSimpleString() {

        var person = pojolizer.pojoify(xml, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("Dumildes Paulo", person.getName());
        Assertions.assertEquals(77, person.getAge());
    }

    @DisplayName("Should returns an instance")
    @Test
    @SneakyThrows
    void shouldReturnsAnInstance() {

        var person = pojolizer.pojoify(xml, Person.class);

        Assertions.assertNotNull(person);
    }

    @DisplayName("Should returns a class with a list of person")
    @Test
    @SneakyThrows
    void shouldReturnsAClassWithAListOfPerson() {
        var clazz = pojolizer.pojoify(clazzXml, Clazz.class);

        Assertions.assertEquals(2, clazz.getPerson().size());
        Assertions.assertEquals("Dumildes Paulo", clazz.getPerson().get(0).getName());
        Assertions.assertEquals(77, clazz.getPerson().get(0).getAge());

    }

    @DisplayName("Should parse a complex object from xml string")
    @Test
    @SneakyThrows
    void shouldParseAComplexObjectFromXMLString() {
        var employee = pojolizer.pojoify(employeeXml, Employee.class);
        var person = pojolizer.pojoify(xml, Person.class);

        Assertions.assertNotNull(employee);
        Assertions.assertNotNull(employee.getPerson());
        Assertions.assertEquals("Dumildes Paulo", employee.getPerson().getName());
        Assertions.assertEquals(person, employee.getPerson());

    }

    @DisplayName("Should parse company object from xml string")
    @Test
    @SneakyThrows
    void shouldParseCompanyObjectFromXMLString() {
        var employee = pojolizer.pojoify(employeeXml, Employee.class);
        var company = pojolizer.pojoify(companyXml, Company.class);

        System.out.println("Employee: " + company.getEmployee());

        Assertions.assertNotNull(company);
        Assertions.assertNotNull(company.getEmployee());

    }

    @Test
    @SneakyThrows
    void shouldThrowsSAXParseException() {
        Assertions.assertThrows(SAXParseException.class, () -> pojolizer.pojoify("", Person.class));
    }

}
