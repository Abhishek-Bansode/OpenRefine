
package com.google.refine.operations.cell;

import static org.testng.Assert.assertThrows;

import java.io.Serializable;

import com.fasterxml.jackson.databind.node.TextNode;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationDescription;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.util.TestUtils;

public class TransposeColumnsIntoRowsOperationTest extends RefineTest {

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "transpose-columns-into-rows", TransposeColumnsIntoRowsOperation.class);
    }

    @Test
    public void serializeTransposeColumnsIntoRowsTestsFixedLength() throws Exception {
        String json = "{" +
                "  \"columnCount\" : 2," +
                "  \"combinedColumnName\" : \"b\"," +
                "  \"description\" : "
                + new TextNode(OperationDescription.cell_transpose_columns_into_rows_combined_pos_brief(2, "b 1", "b")).toString() + "," +
                "  \"fillDown\" : false," +
                "  \"ignoreBlankCells\" : true," +
                "  \"keyColumnName\" : null," +
                "  \"op\" : \"core/transpose-columns-into-rows\"," +
                "  \"prependColumnName\" : false," +
                "  \"separator\" : null," +
                "  \"startColumnName\" : \"b 1\"," +
                "  \"valueColumnName\" : null" +
                "}";
        TestUtils.isSerializedTo(new TransposeColumnsIntoRowsOperation(
                "b 1", 2, true, false, "b", false, null), json);
    }

    @Test
    public void testValidate() {
        assertThrows(IllegalArgumentException.class,
                () -> new TransposeColumnsIntoRowsOperation(null, -1, true, false, "a", true, ":").validate());
        assertThrows(IllegalArgumentException.class, () -> new TransposeColumnsIntoRowsOperation(
                "b 1", 2, true, false, null, "value").validate());
        assertThrows(IllegalArgumentException.class, () -> new TransposeColumnsIntoRowsOperation(
                "b 1", 2, true, false, "key", null).validate());
    }

    @Test
    public void testCreateHistoryEntry_transposeIntoOneColumn_removeRowForNullOrEmptyCell() throws Exception {
        Project project = createProject(
                new String[] { "num1", "num2" },
                new Serializable[][] {
                        { "2", "3" },
                        { "6", null },
                        { "5", "9" }
                });

        AbstractOperation op = new TransposeColumnsIntoRowsOperation("num1", -1, true, false, "a", true, ":");

        runOperation(op, project);

        Project expectedProject = createProject(
                new String[] { "a" },
                new Serializable[][] {
                        { "num1:2" },
                        { "num2:3" },
                        { "num1:6" },
                        { "num1:5" },
                        { "num2:9" },
                });
        assertProjectEquals(project, expectedProject);
    }

    /**
     * This shows how the transpose columns into rows operation can, in certain cases, be an inverse to the transpose
     * rows into columns operation.
     */
    @Test
    public void testTransposeBackToRecords() throws Exception {
        Project project = createProject(
                new String[] { "a", "b 1", "b 2", "c" },
                new Serializable[][] {
                        { "1", "2", "5", "3" },
                        { "7", "8", "11", "9" }
                });

        AbstractOperation op = new TransposeColumnsIntoRowsOperation(
                "b 1", 2, true, false, "b", false, null);

        runOperation(op, project);

        Project expectedProject = createProject(
                new String[] { "a", "b", "c" },
                new Serializable[][] {
                        { "1", "2", "3" },
                        { null, "5", null },
                        { "7", "8", "9" },
                        { null, "11", null }
                });

        assertProjectEquals(project, expectedProject);
    }

    @Test
    public void testTransposeBackToRecordsNoLimit() throws Exception {
        Project project = createProject(
                new String[] { "a", "b 1", "b 2", "c" },
                new Serializable[][] {
                        { "1", "2", "5", "3" },
                        { "7", "8", "11", "9" }
                });

        AbstractOperation op = new TransposeColumnsIntoRowsOperation(
                "b 1", 0, true, false, "b", false, null);

        runOperation(op, project);

        Project expectedProject = createProject(
                new String[] { "a", "b" },
                new Serializable[][] {
                        { "1", "2" },
                        { null, "5" },
                        { null, "3" },
                        { "7", "8", },
                        { null, "11" },
                        { null, "9" }
                });

        assertProjectEquals(project, expectedProject);
    }

    @Test
    public void testTransposeBackToRecordsKeyValue() throws Exception {
        Project project = createProject(
                new String[] { "a", "b 1", "b 2", "c" },
                new Serializable[][] {
                        { "1", "2", "5", "3" },
                        { "7", "8", "11", "9" }
                });

        AbstractOperation op = new TransposeColumnsIntoRowsOperation(
                "b 1", 2, true, false, "key", "value");

        runOperation(op, project);

        Project expectedProject = createProject(
                new String[] { "a", "key", "value", "c" },
                new Serializable[][] {
                        { "1", "b 1", "2", "3" },
                        { null, "b 2", "5", null },
                        { "7", "b 1", "8", "9" },
                        { null, "b 2", "11", null }
                });

        assertProjectEquals(project, expectedProject);
    }

    @Test
    public void testBlankValues() throws Exception {
        Project project = createProject(
                new String[] { "num1", "num2" },
                new Serializable[][] {
                        { "2", "3" },
                        { "6", "", },
                        { "5", "9" }
                });

        AbstractOperation op = new TransposeColumnsIntoRowsOperation(
                "num1", -1, true, false, "a", true, ":");

        runOperation(op, project);

        Project expectedProject = createProject(
                new String[] { "a" },
                new Serializable[][] {
                        { "num1:2" },
                        { "num2:3" },
                        { "num1:6" },
                        { "num1:5" },
                        { "num2:9" }
                });

        assertProjectEquals(project, expectedProject);
    }
}
