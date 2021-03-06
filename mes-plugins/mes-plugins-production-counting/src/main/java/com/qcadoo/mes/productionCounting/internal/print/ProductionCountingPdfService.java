/**
 * ***************************************************************************

 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 1.2.0
 *
 * This file is part of Qcadoo.
 *
 * Qcadoo is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * ***************************************************************************
 */
package com.qcadoo.mes.productionCounting.internal.print;

import static com.qcadoo.mes.productionCounting.internal.constants.OrderFieldsPC.TYPE_OF_PRODUCTION_RECORDING;
import static com.qcadoo.mes.productionCounting.internal.constants.ProductionRecordFields.STATE;
import static com.qcadoo.mes.productionCounting.states.constants.ProductionRecordState.ACCEPTED;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.qcadoo.localization.api.TranslationService;
import com.qcadoo.localization.api.utils.DateUtils;
import com.qcadoo.mes.basic.constants.ProductFields;
import com.qcadoo.mes.orders.constants.OrderFields;
import com.qcadoo.mes.productionCounting.internal.constants.ProductionCountingConstants;
import com.qcadoo.mes.productionCounting.internal.constants.ProductionCountingFields;
import com.qcadoo.mes.productionCounting.internal.constants.ProductionRecordFields;
import com.qcadoo.mes.productionCounting.internal.constants.RecordOperationProductInComponentFields;
import com.qcadoo.mes.productionCounting.internal.constants.RecordOperationProductOutComponentFields;
import com.qcadoo.mes.productionCounting.internal.print.utils.EntityProductInOutComparator;
import com.qcadoo.mes.productionCounting.internal.print.utils.EntityProductionRecordComparator;
import com.qcadoo.mes.technologies.TechnologyService;
import com.qcadoo.mes.technologies.constants.OperationFields;
import com.qcadoo.mes.technologies.constants.TechnologyInstanceOperCompFields;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.NumberService;
import com.qcadoo.model.api.search.SearchRestrictions;
import com.qcadoo.report.api.FontUtils;
import com.qcadoo.report.api.pdf.HeaderAlignment;
import com.qcadoo.report.api.pdf.PdfDocumentService;
import com.qcadoo.report.api.pdf.PdfHelper;
import com.qcadoo.view.api.utils.TimeConverterService;

@Service
public class ProductionCountingPdfService extends PdfDocumentService {

    private static final String TAB_SPACE_LITERAL = "\t \t \t";

    private static final String FIELD_USED_QUANTITY = "usedQuantity";

    private static final String NOT_AVAILABLE = "";

    private static final String QCADOO_VIEW_FALSE = "qcadooView.false";

    private static final String QCADOO_VIEW_TRUE = "qcadooView.true";

    @Autowired
    private DataDefinitionService dataDefinitionService;

    @Autowired
    private TimeConverterService timeConverterService;

    @Autowired
    private TechnologyService technologyService;

    @Autowired
    private TranslationService translationService;

    @Autowired
    private NumberService numberService;

    @Autowired
    private PdfHelper pdfHelper;

    @Override
    protected void buildPdfContent(final Document document, final Entity productionCounting, final Locale locale)
            throws DocumentException {
        final String documentTitle = translationService.translate("productionCounting.productionCounting.report.title", locale)
                + " " + productionCounting.getId().toString();
        final String documentAuthor = translationService.translate("qcadooReport.commons.generatedBy.label", locale);
        pdfHelper.addDocumentHeader(document, "", documentTitle, documentAuthor, (Date) productionCounting.getField("date"));

        final PdfPTable leftPanel = createLeftPanel(productionCounting, locale);
        final PdfPTable rightPanel = createRightPanel(productionCounting, locale);

        final PdfPTable panelTable = pdfHelper.createPanelTable(2);

        panelTable.addCell(leftPanel);
        panelTable.addCell(rightPanel);
        panelTable.setSpacingAfter(20);
        panelTable.setSpacingBefore(20);
        document.add(panelTable);
        final List<Entity> productionRecordsList = dataDefinitionService
                .get(ProductionCountingConstants.PLUGIN_IDENTIFIER, ProductionCountingConstants.MODEL_PRODUCTION_RECORD)
                .find()
                .add(SearchRestrictions.eq(STATE, ACCEPTED.getStringValue()))
                .add(SearchRestrictions.belongsTo(ProductionRecordFields.ORDER,
                        productionCounting.getBelongsToField(ProductionCountingFields.ORDER))).list().getEntities();
        Collections.sort(productionRecordsList, new EntityProductionRecordComparator());
        for (Entity productionRecord : productionRecordsList) {
            addProductionRecord(document, productionRecord, locale);
        }
    }

    private void addTableCellAsTable(final PdfPTable table, final String label, final Object fieldValue, final String nullValue,
            final Font headerFont, final Font valueFont, final DecimalFormat df) {
        final PdfPTable cellTable = new PdfPTable(2);
        cellTable.getDefaultCell().setBorder(PdfPCell.NO_BORDER);
        cellTable.addCell(new Phrase(label, headerFont));
        final Object value = fieldValue;
        if (value == null) {
            cellTable.addCell(new Phrase(nullValue, valueFont));
        } else {
            if (value instanceof BigDecimal && df != null) {
                cellTable.addCell(new Phrase(df.format(value), valueFont));
            } else {
                cellTable.addCell(new Phrase(value.toString(), valueFont));
            }
        }
        table.addCell(cellTable);
    }

    private PdfPTable createLeftPanel(final Entity productionCounting, final Locale locale) {
        final PdfPTable leftPanel = pdfHelper.createPanelTableWithSimpleFormat(1);

        addTableCellAsTable(leftPanel, translationService.translate("productionCounting.productionCounting.report.title", locale)
                + ":", productionCounting.getId().toString(), null, FontUtils.getDejavuBold9Dark(),
                FontUtils.getDejavuBold9Dark(), null);
        addTableCellAsTable(leftPanel,
                translationService.translate("productionCounting.productionBalance.report.panel.order", locale),
                productionCounting.getBelongsToField(ProductionCountingFields.ORDER).getStringField(OrderFields.NAME), null,
                FontUtils.getDejavuBold9Dark(), FontUtils.getDejavuBold9Dark(), null);
        addTableCellAsTable(leftPanel,
                translationService.translate("productionCounting.productionBalance.report.panel.product", locale),
                productionCounting.getBelongsToField(ProductionCountingFields.PRODUCT).getStringField(ProductFields.NAME), null,
                FontUtils.getDejavuBold9Dark(), FontUtils.getDejavuBold9Dark(), null);
        addTableCellAsTable(
                leftPanel,
                translationService.translate("productionCounting.productionBalance.report.panel.numberOfRecords", locale),
                String.valueOf(productionCounting.getBelongsToField(ProductionCountingFields.ORDER)
                        .getHasManyField("productionRecords").size()), null, FontUtils.getDejavuBold9Dark(),
                FontUtils.getDejavuBold9Dark(), null);
        addTableCellAsTable(leftPanel,
                translationService.translate("productionCounting.productionBalance.description.label", locale) + ":",
                productionCounting.getStringField("description"), null, FontUtils.getDejavuBold9Dark(),
                FontUtils.getDejavuBold9Dark(), null);

        return leftPanel;
    }

    private PdfPTable createRightPanel(final Entity productionCounting, final Locale locale) {
        final PdfPTable rightPanel = pdfHelper.createPanelTableWithSimpleFormat(1);

        rightPanel.addCell(new Phrase(translationService.translate(
                "costCalculation.costCalculationDetails.window.mainTab.form.parameters", locale) + ":", FontUtils
                .getDejavuBold10Dark()));
        rightPanel.addCell(new Phrase(TAB_SPACE_LITERAL
                + translationService.translate("productionCounting.productionBalance.report.panel.registerQuantityOutProduct",
                        locale)
                + " "
                + ((Boolean) productionCounting.getBelongsToField(ProductionCountingFields.ORDER).getField(
                        "registerQuantityInProduct") ? translationService.translate(QCADOO_VIEW_TRUE, locale)
                        : translationService.translate(QCADOO_VIEW_FALSE, locale)), FontUtils.getDejavuBold9Dark()));
        rightPanel.addCell(new Phrase(TAB_SPACE_LITERAL
                + translationService.translate("productionCounting.productionBalance.report.panel.registerQuantityInProduct",
                        locale)
                + " "
                + ((Boolean) productionCounting.getBelongsToField(ProductionCountingFields.ORDER).getField(
                        "registerQuantityOutProduct") ? translationService.translate(QCADOO_VIEW_TRUE, locale)
                        : translationService.translate(QCADOO_VIEW_FALSE, locale)), FontUtils.getDejavuBold9Dark()));
        rightPanel.addCell(new Phrase(TAB_SPACE_LITERAL
                + translationService
                        .translate("productionCounting.productionBalance.report.panel.registerProductionTime", locale)
                + " "
                + ((Boolean) productionCounting.getBelongsToField(ProductionCountingFields.ORDER).getField(
                        "registerProductionTime") ? translationService.translate(QCADOO_VIEW_TRUE, locale) : translationService
                        .translate(QCADOO_VIEW_FALSE, locale)), FontUtils.getDejavuBold9Dark()));
        rightPanel
                .addCell(new Phrase(
                        TAB_SPACE_LITERAL
                                + translationService.translate("productionCounting.productionBalance.report.panel.justOne",
                                        locale)
                                + " "
                                + ((Boolean) productionCounting.getBelongsToField(ProductionCountingFields.ORDER).getField(
                                        "justOne") ? translationService.translate(QCADOO_VIEW_TRUE, locale) : translationService
                                        .translate(QCADOO_VIEW_FALSE, locale)), FontUtils.getDejavuBold9Dark()));
        rightPanel.addCell(new Phrase(
                TAB_SPACE_LITERAL
                        + translationService.translate("productionCounting.productionBalance.report.panel.allowToClose", locale)
                        + " "
                        + ((Boolean) productionCounting.getBelongsToField(ProductionCountingFields.ORDER)
                                .getField("allowToClose") ? translationService.translate(QCADOO_VIEW_TRUE, locale)
                                : translationService.translate(QCADOO_VIEW_FALSE, locale)), FontUtils.getDejavuBold9Dark()));
        rightPanel.addCell(new Phrase(
                TAB_SPACE_LITERAL
                        + translationService
                                .translate("productionCounting.productionBalance.report.panel.autoCloseOrder", locale)
                        + " "
                        + ((Boolean) productionCounting.getBelongsToField(ProductionCountingFields.ORDER).getField(
                                "autoCloseOrder") ? translationService.translate(QCADOO_VIEW_TRUE, locale) : translationService
                                .translate(QCADOO_VIEW_FALSE, locale)), FontUtils.getDejavuBold9Dark()));

        return rightPanel;
    }

    private void addProductionRecord(final Document document, final Entity productionRecord, final Locale locale)
            throws DocumentException {
        document.add(new Paragraph(translationService.translate("productionCounting.productionCounting.report.paragraph", locale)
                + " " + productionRecord.getStringField("number"), FontUtils.getDejavuBold19Dark()));

        PdfPTable panelTable = pdfHelper.createPanelTable(2);
        addTableCellAsTable(
                panelTable,
                translationService.translate("productionCounting.productionCounting.report.panel.recordType", locale),
                (Boolean) productionRecord.getBooleanField("lastRecord") ? translationService.translate(
                        "productionCounting.productionCounting.report.panel.recordType.final", locale) : translationService
                        .translate("productionCounting.productionCounting.report.panel.recordType.partial", locale), null,
                FontUtils.getDejavuBold9Dark(), FontUtils.getDejavuBold9Dark(), null);
        if (productionRecord.getBelongsToField(ProductionRecordFields.ORDER).getStringField(TYPE_OF_PRODUCTION_RECORDING)
                .equals("02cumulated")) {
            addTableCellAsTable(panelTable,
                    translationService.translate("productionCounting.productionCounting.report.panel.operationAndLevel", locale),
                    NOT_AVAILABLE, null, FontUtils.getDejavuBold9Dark(), FontUtils.getDejavuBold9Dark(), null);
        } else {
            addTableCellAsTable(
                    panelTable,
                    translationService.translate("productionCounting.productionCounting.report.panel.operationAndLevel", locale),
                    productionRecord.getBelongsToField(ProductionRecordFields.TECHNOLOGY_INSTANCE_OPERATION_COMPONENT)
                            .getStringField(TechnologyInstanceOperCompFields.NODE_NUMBER)
                            + " "
                            + productionRecord.getBelongsToField(ProductionRecordFields.TECHNOLOGY_INSTANCE_OPERATION_COMPONENT)
                                    .getBelongsToField(TechnologyInstanceOperCompFields.OPERATION)
                                    .getStringField(OperationFields.NAME), null, FontUtils.getDejavuBold9Dark(),
                    FontUtils.getDejavuBold9Dark(), null);
            addTableCellAsTable(
                    panelTable,
                    translationService.translate("productionCounting.productionCounting.report.panel.dateAndTime", locale),
                    new SimpleDateFormat(DateUtils.L_DATE_TIME_FORMAT, locale).format((Date) productionRecord
                            .getHasManyField(ProductionRecordFields.STATE_CHANGES).get(0).getField("dateAndTime")), null,
                    FontUtils.getDejavuBold9Dark(), FontUtils.getDejavuBold9Dark(), null);
            if ((Boolean) productionRecord.getBelongsToField(ProductionRecordFields.ORDER).getField("registerProductionTime")) {
                addTableCellAsTable(panelTable, translationService.translate(
                        "productionCounting.productionCounting.report.panel.machineOperationTime", locale),
                        timeConverterService.convertTimeToString((Integer) productionRecord.getField("machineTime")), null,
                        FontUtils.getDejavuBold9Dark(), FontUtils.getDejavuBold9Dark(), null);
            } else {
                addTableCellAsTable(panelTable, translationService.translate(
                        "productionCounting.productionCounting.report.panel.machineOperationTime", locale), NOT_AVAILABLE, null,
                        FontUtils.getDejavuBold9Dark(), FontUtils.getDejavuBold9Dark(), null);
            }
        }
        addTableCellAsTable(panelTable,
                translationService.translate("productionCounting.productionCounting.report.panel.worker", locale),
                productionRecord.getHasManyField(ProductionRecordFields.STATE_CHANGES).get(0).getStringField("worker"), null,
                FontUtils.getDejavuBold9Dark(), FontUtils.getDejavuBold9Dark(), null);
        if ((Boolean) productionRecord.getBelongsToField(ProductionRecordFields.ORDER).getField("registerProductionTime")) {
            addTableCellAsTable(
                    panelTable,
                    translationService.translate("productionCounting.productionCounting.report.panel.laborOperationTime", locale),
                    timeConverterService.convertTimeToString((Integer) productionRecord.getField("laborTime")), null,
                    FontUtils.getDejavuBold9Dark(), FontUtils.getDejavuBold9Dark(), null);
        } else {
            addTableCellAsTable(
                    panelTable,
                    translationService.translate("productionCounting.productionCounting.report.panel.laborOperationTime", locale),
                    NOT_AVAILABLE, null, FontUtils.getDejavuBold9Dark(), FontUtils.getDejavuBold9Dark(), null);
        }
        panelTable.setSpacingBefore(10);
        document.add(panelTable);

        Entity technology = productionRecord.getBelongsToField(ProductionRecordFields.ORDER).getBelongsToField("technology");

        if ((Boolean) productionRecord.getBelongsToField(ProductionRecordFields.ORDER).getField("registerQuantityInProduct")) {
            addInputProducts(document, productionRecord, technology, locale);
        }

        if ((Boolean) productionRecord.getBelongsToField(ProductionRecordFields.ORDER).getField("registerQuantityOutProduct")) {
            addOutputProducts(document, productionRecord, technology, locale);
        }
    }

    private void addInputProducts(final Document document, final Entity productionRecord, final Entity technology,
            final Locale locale) throws DocumentException {
        document.add(new Paragraph(translationService
                .translate("productionCounting.productionCounting.report.paragraph2", locale), FontUtils.getDejavuBold11Dark()));

        List<String> inputProductsTableHeader = new ArrayList<String>();
        inputProductsTableHeader.add(translationService.translate(
                "productionCounting.productionBalance.report.columnHeader.number", locale));
        inputProductsTableHeader.add(translationService.translate(
                "productionCounting.productionBalance.report.columnHeader.productionName", locale));
        inputProductsTableHeader.add(translationService.translate(
                "productionCounting.productionBalance.report.columnHeader.type", locale));
        inputProductsTableHeader.add(translationService.translate(
                "productionCounting.productionCounting.report.columnHeader.quantity", locale));
        inputProductsTableHeader.add(translationService.translate("basic.product.unit.label", locale));

        Map<String, HeaderAlignment> alignments = Maps.newHashMap();

        alignments.put(translationService.translate("productionCounting.productionBalance.report.columnHeader.number", locale),
                HeaderAlignment.LEFT);
        alignments.put(
                translationService.translate("productionCounting.productionBalance.report.columnHeader.productionName", locale),
                HeaderAlignment.LEFT);
        alignments.put(translationService.translate("productionCounting.productionBalance.report.columnHeader.type", locale),
                HeaderAlignment.LEFT);
        alignments.put(
                translationService.translate("productionCounting.productionCounting.report.columnHeader.quantity", locale),
                HeaderAlignment.RIGHT);
        alignments.put(translationService.translate("basic.product.unit.label", locale), HeaderAlignment.LEFT);

        PdfPTable inputProductsTable = pdfHelper.createTableWithHeader(5, inputProductsTableHeader, false, alignments);

        if (productionRecord.getHasManyField("recordOperationProductInComponents") != null) {
            List<Entity> productsInList = new ArrayList<Entity>(
                    productionRecord.getHasManyField(ProductionRecordFields.RECORD_OPERATION_PRODUCT_IN_COMPONENTS));
            Collections.sort(productsInList, new EntityProductInOutComparator());
            for (Entity productIn : productsInList) {
                inputProductsTable.addCell(new Phrase(productIn
                        .getBelongsToField(RecordOperationProductInComponentFields.PRODUCT).getStringField(ProductFields.NUMBER),
                        FontUtils.getDejavuRegular9Dark()));
                inputProductsTable.addCell(new Phrase(productIn
                        .getBelongsToField(RecordOperationProductInComponentFields.PRODUCT).getStringField(ProductFields.NAME),
                        FontUtils.getDejavuRegular9Dark()));

                String type = technologyService.getProductType(
                        productIn.getBelongsToField(RecordOperationProductInComponentFields.PRODUCT), technology);
                inputProductsTable.addCell(new Phrase(translationService.translate("basic.product.globalTypeOfMaterial.value."
                        + type, locale), FontUtils.getDejavuRegular9Dark()));

                inputProductsTable.getDefaultCell().setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
                if (productIn.getField(FIELD_USED_QUANTITY) == null) {
                    inputProductsTable.addCell(new Phrase(NOT_AVAILABLE, FontUtils.getDejavuRegular9Dark()));
                } else {
                    inputProductsTable.addCell(new Phrase(numberService.format(productIn.getField(FIELD_USED_QUANTITY)),
                            FontUtils.getDejavuRegular9Dark()));
                }
                inputProductsTable.getDefaultCell().setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
                inputProductsTable.addCell(new Phrase(productIn
                        .getBelongsToField(RecordOperationProductInComponentFields.PRODUCT).getStringField("unit"), FontUtils
                        .getDejavuRegular9Dark()));
            }
        }

        document.add(inputProductsTable);
    }

    private void addOutputProducts(final Document document, final Entity productionRecord, final Entity technology,
            final Locale locale) throws DocumentException {
        document.add(new Paragraph(translationService
                .translate("productionCounting.productionCounting.report.paragraph3", locale), FontUtils.getDejavuBold11Dark()));

        List<String> outputProductsTableHeader = new ArrayList<String>();
        outputProductsTableHeader.add(translationService.translate(
                "productionCounting.productionBalance.report.columnHeader.number", locale));
        outputProductsTableHeader.add(translationService.translate(
                "productionCounting.productionBalance.report.columnHeader.productionName", locale));
        outputProductsTableHeader.add(translationService.translate(
                "productionCounting.productionBalance.report.columnHeader.type", locale));
        outputProductsTableHeader.add(translationService.translate(
                "productionCounting.productionCounting.report.columnHeader.quantity", locale));
        outputProductsTableHeader.add(translationService.translate("basic.product.unit.label", locale));
        Map<String, HeaderAlignment> alignments = Maps.newHashMap();

        alignments.put(translationService.translate("productionCounting.productionBalance.report.columnHeader.number", locale),
                HeaderAlignment.LEFT);
        alignments.put(
                translationService.translate("productionCounting.productionBalance.report.columnHeader.productionName", locale),
                HeaderAlignment.LEFT);
        alignments.put(translationService.translate("productionCounting.productionBalance.report.columnHeader.type", locale),
                HeaderAlignment.LEFT);
        alignments.put(
                translationService.translate("productionCounting.productionCounting.report.columnHeader.quantity", locale),
                HeaderAlignment.RIGHT);
        alignments.put(translationService.translate("basic.product.unit.label", locale), HeaderAlignment.LEFT);
        PdfPTable outputProductsTable = pdfHelper.createTableWithHeader(5, outputProductsTableHeader, false, alignments);

        if (productionRecord.getHasManyField("recordOperationProductOutComponents") != null) {
            List<Entity> productsOutList = new ArrayList<Entity>(
                    productionRecord.getHasManyField("recordOperationProductOutComponents"));
            Collections.sort(productsOutList, new EntityProductInOutComparator());
            for (Entity productOut : productsOutList) {
                outputProductsTable.addCell(new Phrase(productOut.getBelongsToField(
                        RecordOperationProductOutComponentFields.PRODUCT).getStringField(ProductFields.NUMBER), FontUtils
                        .getDejavuRegular9Dark()));
                outputProductsTable.addCell(new Phrase(productOut.getBelongsToField(
                        RecordOperationProductOutComponentFields.PRODUCT).getStringField(ProductFields.NAME), FontUtils
                        .getDejavuRegular9Dark()));

                String type = technologyService.getProductType(
                        productOut.getBelongsToField(RecordOperationProductOutComponentFields.PRODUCT), technology);
                outputProductsTable.addCell(new Phrase(translationService.translate("basic.product.globalTypeOfMaterial.value."
                        + type, locale), FontUtils.getDejavuRegular9Dark()));

                outputProductsTable.getDefaultCell().setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
                if (productOut.getField(FIELD_USED_QUANTITY) == null) {
                    outputProductsTable.addCell(new Phrase(NOT_AVAILABLE, FontUtils.getDejavuRegular9Dark()));
                } else {
                    outputProductsTable.addCell(new Phrase(numberService.format(productOut.getField(FIELD_USED_QUANTITY)),
                            FontUtils.getDejavuRegular9Dark()));
                }
                outputProductsTable.getDefaultCell().setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
                outputProductsTable.addCell(new Phrase(productOut.getBelongsToField(
                        RecordOperationProductOutComponentFields.PRODUCT).getStringField("unit"), FontUtils
                        .getDejavuRegular9Dark()));
            }
        }

        document.add(outputProductsTable);
    }

    @Override
    public String getReportTitle(final Locale locale) {
        return translationService.translate("productionCounting.productionBalance.report.title", locale);
    }

}
