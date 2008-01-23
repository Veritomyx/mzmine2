/*
 * Copyright 2006-2007 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.peaklist;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.dataanalysis.intensityplot.IntensityPlot;
import net.sf.mzmine.modules.dataanalysis.intensityplot.IntensityPlotDialog;
import net.sf.mzmine.modules.dataanalysis.intensityplot.IntensityPlotFrame;
import net.sf.mzmine.modules.dataanalysis.intensityplot.IntensityPlotParameters;
import net.sf.mzmine.modules.visualization.peaklist.table.CommonColumnType;
import net.sf.mzmine.modules.visualization.peaklist.table.DataFileColumnType;
import net.sf.mzmine.modules.visualization.peaklist.table.PeakListTable;
import net.sf.mzmine.modules.visualization.peaklist.table.PeakListTableColumnModel;
import net.sf.mzmine.modules.visualization.peaklist.table.PeakListTableModel;
import net.sf.mzmine.modules.visualization.tic.TICVisualizer;
import net.sf.mzmine.modules.visualization.tic.TICVisualizerParameters;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.dialogs.ExitCode;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.NumberFormatter;

import com.sun.java.TableSorter;

/**
 * 
 */
public class PeakListTablePopupMenu extends JPopupMenu implements
        ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private PeakListTable table;
    private PeakList peakList;
    private PeakListTableColumnModel columnModel;

    private JMenuItem deleteRowsItem, addNewRowItem, plotRowsItem, showXICItem,
            manuallyDefineItem;

    private RawDataFile clickedDataFile;
    private PeakListRow clickedPeakListRow;

    public PeakListTablePopupMenu(PeakListTableWindow window,
            PeakListTable table, PeakListTableColumnModel columnModel,
            PeakList peakList) {

        this.table = table;
        this.peakList = peakList;
        this.columnModel = columnModel;

        GUIUtils.addMenuItem(this, "Set properties", window, "PROPERTIES");

        deleteRowsItem = GUIUtils.addMenuItem(this, "Delete selected rows",
                this);

        addNewRowItem = GUIUtils.addMenuItem(this, "Add new row", this);

        plotRowsItem = GUIUtils.addMenuItem(this,
                "Plot selected rows using Intensity Plot module", this);

        showXICItem = GUIUtils.addMenuItem(this, "Show XIC of this peak", this);

        manuallyDefineItem = GUIUtils.addMenuItem(this, "Manually define peak",
                this);

    }

    public void show(Component invoker, int x, int y) {

        int selectedRows[] = table.getSelectedRows();

        deleteRowsItem.setEnabled(selectedRows.length > 0);
        plotRowsItem.setEnabled(selectedRows.length > 0);

        Point clickedPoint = new Point(x, y);
        int clickedRow = table.rowAtPoint(clickedPoint);
        int clickedColumn = columnModel.getColumn(
                table.columnAtPoint(clickedPoint)).getModelIndex();
        if ((clickedRow >= 0) && (clickedColumn >= 0)) {
            showXICItem.setEnabled(clickedColumn >= CommonColumnType.values().length);
            manuallyDefineItem.setEnabled(clickedColumn >= CommonColumnType.values().length);
            int dataFileIndex = (clickedColumn - CommonColumnType.values().length)
                    / DataFileColumnType.values().length;
            clickedDataFile = peakList.getRawDataFile(dataFileIndex);
            TableSorter sorter = (TableSorter) table.getModel();
            clickedPeakListRow = peakList.getRow(sorter.modelIndex(clickedRow));
        }

        super.show(invoker, x, y);
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {

        Object src = event.getSource();

        if (src == deleteRowsItem) {

            int rowsToDelete[] = table.getSelectedRows();
            // sort row indices
            Arrays.sort(rowsToDelete);
            TableSorter sorterModel = (TableSorter) table.getModel();
            PeakListTableModel originalModel = (PeakListTableModel) sorterModel.getTableModel();

            // delete the rows starting from last
            for (int i = rowsToDelete.length - 1; i >= 0; i--) {
                int unsordedIndex = sorterModel.modelIndex(rowsToDelete[i]);
                peakList.removeRow(unsordedIndex);
                originalModel.fireTableRowsDeleted(unsordedIndex, unsordedIndex);
            }

            table.clearSelection();

        }

        if (src == plotRowsItem) {

            int selectedTableRows[] = table.getSelectedRows();
            TableSorter sorterModel = (TableSorter) table.getModel();
            PeakListRow selectedPeakListRows[] = new PeakListRow[selectedTableRows.length];
            for (int i = 0; i < selectedTableRows.length; i++) {
                int unsortedIndex = sorterModel.modelIndex(selectedTableRows[i]);
                selectedPeakListRows[i] = peakList.getRow(unsortedIndex);
            }
            IntensityPlot intensityPlotModule = IntensityPlot.getInstance();
            IntensityPlotParameters currentParameters = intensityPlotModule.getParameterSet();
            IntensityPlotParameters newParameters = new IntensityPlotParameters(
                    peakList, currentParameters.getXAxisValueSource(),
                    currentParameters.getYAxisValueSource(),
                    peakList.getRawDataFiles(), selectedPeakListRows);
            IntensityPlotDialog setupDialog = new IntensityPlotDialog(peakList,
                    newParameters);
            setupDialog.setVisible(true);
            if (setupDialog.getExitCode() == ExitCode.OK) {
                intensityPlotModule.setParameters(newParameters);
                Desktop desktop = MZmineCore.getDesktop();
                logger.info("Opening new intensity plot");
                IntensityPlotFrame newFrame = new IntensityPlotFrame(
                        newParameters);
                desktop.addInternalFrame(newFrame);
            }

        }

        if (src == showXICItem) {

            Peak clickedPeak = clickedPeakListRow.getPeak(clickedDataFile);
            TICVisualizer tic = TICVisualizer.getInstance();

            float rtMin = clickedDataFile.getDataMinRT(1);
            float rtMax = clickedDataFile.getDataMaxRT(1);

            if (clickedPeak != null) {
                tic.showNewTICVisualizerWindow(
                        new RawDataFile[] { clickedDataFile },
                        new Peak[] { clickedPeak }, 1,
                        TICVisualizerParameters.plotTypeBP, rtMin, rtMax,
                        clickedPeak.getRawDataPointMinMZ(),
                        clickedPeak.getRawDataPointMaxMZ());

            } else {
                float minMZ = clickedPeakListRow.getAverageMZ();
                float maxMZ = clickedPeakListRow.getAverageMZ();
                for (Peak peak : clickedPeakListRow.getPeaks()) {
                    if (peak == null)
                        continue;
                    if (peak.getRawDataPointMinMZ() < minMZ)
                        minMZ = peak.getRawDataPointMinMZ();
                    if (peak.getRawDataPointMaxMZ() > maxMZ)
                        maxMZ = peak.getRawDataPointMaxMZ();
                }
                tic.showNewTICVisualizerWindow(
                        new RawDataFile[] { clickedDataFile }, null, 1,
                        TICVisualizerParameters.plotTypeBP, rtMin, rtMax,
                        minMZ, maxMZ);
            }

        }

        if (src == manuallyDefineItem) {

            Peak clickedPeak = clickedPeakListRow.getPeak(clickedDataFile);
            float minRT, maxRT, minMZ, maxMZ;
            if (clickedPeak != null) {
                minRT = clickedPeak.getRawDataPointMinRT();
                maxRT = clickedPeak.getRawDataPointMaxRT();
                minMZ = clickedPeak.getRawDataPointMinMZ();
                maxMZ = clickedPeak.getRawDataPointMaxMZ();
            } else {
                minRT = clickedPeakListRow.getAverageRT();
                maxRT = clickedPeakListRow.getAverageRT();
                minMZ = clickedPeakListRow.getAverageMZ();
                maxMZ = clickedPeakListRow.getAverageMZ();

                for (Peak peak : clickedPeakListRow.getPeaks()) {
                    if (peak == null)
                        continue;
                    if (peak.getRawDataPointMinRT() < minRT)
                        minRT = peak.getRawDataPointMinRT();
                    if (peak.getRawDataPointMaxRT() > maxRT)
                        maxRT = peak.getRawDataPointMaxRT();
                    if (peak.getRawDataPointMinMZ() < minMZ)
                        minMZ = peak.getRawDataPointMinMZ();
                    if (peak.getRawDataPointMaxMZ() > maxMZ)
                        maxMZ = peak.getRawDataPointMaxMZ();
                }
            }

            NumberFormatter mzFormat = MZmineCore.getDesktop().getMZFormat();
            NumberFormatter rtFormat = MZmineCore.getDesktop().getRTFormat();

            Parameter minRTparam = new SimpleParameter(ParameterType.FLOAT,
                    "Retention time min", "Retention time min", "s", minRT,
                    clickedDataFile.getDataMinRT(1),
                    clickedDataFile.getDataMaxRT(1), rtFormat);
            Parameter maxRTparam = new SimpleParameter(ParameterType.FLOAT,
                    "Retention time max", "Retention time max", "s", maxRT,
                    clickedDataFile.getDataMinRT(1),
                    clickedDataFile.getDataMaxRT(1), rtFormat);
            Parameter minMZparam = new SimpleParameter(ParameterType.FLOAT,
                    "m/z min", "m/z min", "Da", minMZ,
                    clickedDataFile.getDataMinMZ(1),
                    clickedDataFile.getDataMaxMZ(1), mzFormat);
            Parameter maxMZparam = new SimpleParameter(ParameterType.FLOAT,
                    "m/z max", "m/z max", "Da", maxMZ,
                    clickedDataFile.getDataMinMZ(1),
                    clickedDataFile.getDataMaxMZ(1), mzFormat);
            Parameter[] params = { minRTparam, maxRTparam, minMZparam,
                    maxMZparam };

            SimpleParameterSet parameterSet = new SimpleParameterSet(params);

            ParameterSetupDialog parameterSetupDialog = new ParameterSetupDialog(
                    "Please set peak boundaries", parameterSet);

            parameterSetupDialog.setVisible(true);

            if (parameterSetupDialog.getExitCode() != ExitCode.OK)
                return;

            minRT = (Float) parameterSet.getParameterValue(minRTparam);
            maxRT = (Float) parameterSet.getParameterValue(maxRTparam);
            minMZ = (Float) parameterSet.getParameterValue(minMZparam);
            maxMZ = (Float) parameterSet.getParameterValue(maxMZparam);

            ManuallyDefinePeakTask task = new ManuallyDefinePeakTask(
                    clickedPeakListRow, clickedDataFile, minRT, maxRT, minMZ,
                    maxMZ);

            MZmineCore.getTaskController().addTask(task);
        }

        if (src == addNewRowItem) {

            NumberFormatter mzFormat = MZmineCore.getDesktop().getMZFormat();
            NumberFormatter rtFormat = MZmineCore.getDesktop().getRTFormat();

            Parameter minRTparam = new SimpleParameter(ParameterType.FLOAT,
                    "Retention time min", "Retention time min", "s",
                    (Float) 0f, (Float) 0f, null, rtFormat);
            Parameter maxRTparam = new SimpleParameter(ParameterType.FLOAT,
                    "Retention time max", "Retention time max", "s",
                    (Float) 0f, (Float) 0f, null, rtFormat);
            Parameter minMZparam = new SimpleParameter(ParameterType.FLOAT,
                    "m/z min", "m/z min", "Da", (Float) 0f, (Float) 0f, null,
                    mzFormat);
            Parameter maxMZparam = new SimpleParameter(ParameterType.FLOAT,
                    "m/z max", "m/z max", "Da", (Float) 0f, (Float) 0f, null,
                    mzFormat);
            Parameter[] params = { minRTparam, maxRTparam, minMZparam,
                    maxMZparam };

            SimpleParameterSet parameterSet = new SimpleParameterSet(params);

            ParameterSetupDialog parameterSetupDialog = new ParameterSetupDialog(
                    "Please set peak boundaries", parameterSet);

            parameterSetupDialog.setVisible(true);

            if (parameterSetupDialog.getExitCode() != ExitCode.OK)
                return;

            float minRT = (Float) parameterSet.getParameterValue(minRTparam);
            float maxRT = (Float) parameterSet.getParameterValue(maxRTparam);
            float minMZ = (Float) parameterSet.getParameterValue(minMZparam);
            float maxMZ = (Float) parameterSet.getParameterValue(maxMZparam);

            // find maximum ID and add 1
            int newID = 1;
            for (PeakListRow row : peakList.getRows()) {
                if (row.getID() >= newID)
                    newID = row.getID() + 1;
            }

            // create new row
            SimplePeakListRow newRow = new SimplePeakListRow(newID);
            peakList.addRow(newRow);
            TableSorter sorterModel = (TableSorter) table.getModel();
            PeakListTableModel originalModel = (PeakListTableModel) sorterModel.getTableModel();
            originalModel.fireTableDataChanged();

            // find peaks for new row
            for (RawDataFile dataFile : peakList.getRawDataFiles()) {
                ManuallyDefinePeakTask task = new ManuallyDefinePeakTask(
                        newRow, dataFile, minRT, maxRT, minMZ, maxMZ);
                MZmineCore.getTaskController().addTask(task);
            }
        }

    }

}
