/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package com.google.refine.operations.recon;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.node.TextNode;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.browsing.Engine.Mode;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.ColumnsDiff;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.Recon.Judgment;
import com.google.refine.model.recon.ReconConfig;
import com.google.refine.model.recon.StandardReconConfig;
import com.google.refine.operations.OperationDescription;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class ReconJudgeSimilarCellsOperationTests extends RefineTest {

    static final EngineConfig ENGINE_CONFIG = EngineConfig.defaultRowBased();

    Project project;
    ReconConfig reconConfig;
    String service = "http://my.service.com/api";
    String identifierSpace = "http://my.service.com/identifierSpace";
    String schemaSpace = "http://my.service.com/schemaSpace";

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
        OperationRegistry.registerOperation(getCoreModule(), "recon-judge-similar-cells", ReconJudgeSimilarCellsOperation.class);
    }

    @BeforeMethod
    public void setupInitialState() throws Exception {
        reconConfig = new StandardReconConfig(service,
                identifierSpace,
                schemaSpace,
                null,
                true,
                Collections.emptyList(),
                5);
        project = createProject(
                new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", new Cell("b", testRecon("e", "h", Recon.Judgment.Matched, 1L)) },
                        { "c", new Cell("b", testRecon("x", "p", Recon.Judgment.New, 2L)) },
                        { "c", new Cell("d", testRecon("b", "j", Recon.Judgment.None)) },
                        { "d", "b" }
                });
        project.columnModel.columns.get(1).setReconConfig(reconConfig);
    }

    @Test
    public void serializeReconJudgeSimilarCellsOperation() throws IOException {
        String json = "{\"op\":\"core/recon-judge-similar-cells\","
                + "\"description\":" + new TextNode(OperationDescription.recon_judge_similar_cells_new_share_brief("foo", "A")).toString()
                + ","
                + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]},"
                + "\"columnName\":\"A\","
                + "\"similarValue\":\"foo\","
                + "\"judgment\":\"new\","
                + "\"shareNewTopics\":true}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ReconJudgeSimilarCellsOperation.class), json);
    }

    @Test
    public void serializeReconJudgeSimilarCellsOperationMatch() throws IOException {
        String json = "{\"op\":\"core/recon-judge-similar-cells\","
                + "\"description\":"
                + new TextNode(OperationDescription.recon_judge_similar_cells_matched_brief("Douglas Adams", "Q42", "A", "foo")).toString()
                + ","
                + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]},"
                + "\"columnName\":\"A\","
                + "\"similarValue\":\"foo\","
                + "\"judgment\":\"matched\","
                + "\"match\":{\"id\":\"Q42\",\"name\":\"Douglas Adams\",\"types\":[\"Q5\"],\"score\":85},"
                + "\"shareNewTopics\":false"
                + "}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ReconJudgeSimilarCellsOperation.class), json);
    }

    @Test
    public void testColumnDependencies() throws Exception {
        AbstractOperation op = new ReconJudgeSimilarCellsOperation(
                ENGINE_CONFIG,
                "A",
                "foo",
                Recon.Judgment.New,
                null, true);
        assertEquals(op.getColumnsDiff(), Optional.of(ColumnsDiff.modifySingleColumn("A")));
        assertEquals(op.getColumnDependencies(), Optional.of(Set.of("A")));
    }

    @Test
    public void testRename() throws Exception {
        String json = "{\"op\":\"core/recon-judge-similar-cells\","
                + "\"description\":" + new TextNode(OperationDescription.recon_judge_similar_cells_new_share_brief("foo", "A")).toString()
                + ","
                + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]},"
                + "\"columnName\":\"A\","
                + "\"similarValue\":\"foo\","
                + "\"judgment\":\"new\","
                + "\"shareNewTopics\":true}";

        var SUT = ParsingUtilities.mapper.readValue(json, ReconJudgeSimilarCellsOperation.class);

        ReconJudgeSimilarCellsOperation renamed = SUT.renameColumns(Map.of("A", "B"));

        String expectedJson = "{\"op\":\"core/recon-judge-similar-cells\","
                + "\"description\":" + new TextNode(OperationDescription.recon_judge_similar_cells_new_share_brief("foo", "B")).toString()
                + ","
                + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]},"
                + "\"columnName\":\"B\","
                + "\"similarValue\":\"foo\","
                + "\"judgment\":\"new\","
                + "\"shareNewTopics\":true}";
        TestUtils.isSerializedTo(renamed, expectedJson);
    }

    @Test
    public void testMarkNewTopics() throws Exception {
        Project project = createProject(
                new String[] { "A", "B" },
                new Serializable[][] {
                        { "foo", "bar" },
                        { "alpha", "beta" }
                });

        Column column = project.columnModel.columns.get(0);
        ReconConfig config = new StandardReconConfig(
                "http://my.database/recon_service",
                "http://my.database/entity/",
                "http://my.database/schema/",
                null,
                null,
                true,
                Collections.emptyList());
        column.setReconConfig(config);

        AbstractOperation op = new ReconJudgeSimilarCellsOperation(
                ENGINE_CONFIG,
                "A",
                "foo",
                Recon.Judgment.New,
                null, true);

        runOperation(op, project);

        Cell cell = project.rows.get(0).cells.get(0);
        assertEquals(Recon.Judgment.New, cell.recon.judgment);
        assertEquals("http://my.database/entity/", cell.recon.identifierSpace);
        assertNull(project.rows.get(1).cells.get(0).recon);
    }

    private Recon newRecon(long historyEntryId, long id, int batchSize) {
        Recon recon = new Recon(id, historyEntryId, Judgment.New, null, null, null, null,
                service, identifierSpace, schemaSpace, "similar", batchSize, -1);
        recon.candidates = null;
        return recon;
    }

    @Test
    public void testReconJudgeSimilarCellsShareTopics() throws Exception {
        AbstractOperation operation = new ReconJudgeSimilarCellsOperation(
                new EngineConfig(Collections.emptyList(), Mode.RowBased), "bar", "b", Judgment.New, null, true);

        runOperation(operation, project);

        long historyEntryId = project.history.getLastPastEntries(1).get(0).id;
        long commonReconId = project.rows.get(0).getCell(1).recon.id;

        Project expected = createProject(
                new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", new Cell("b", newRecon(historyEntryId, commonReconId, 3)) },
                        { "c", new Cell("b", newRecon(historyEntryId, commonReconId, 3)) },
                        { "c", new Cell("d", testRecon("b", "j", Recon.Judgment.None)) },
                        { "d", new Cell("b", newRecon(historyEntryId, commonReconId, 3)) }
                });
        assertProjectEquals(project, expected);
    }
}
