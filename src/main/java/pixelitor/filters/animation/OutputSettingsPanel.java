/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.filters.animation;

import org.jdesktop.swingx.combobox.EnumComboBoxModel;
import pixelitor.io.FileChooser;
import pixelitor.utils.BrowseFilesSupport;
import pixelitor.utils.GridBagHelper;
import pixelitor.utils.TFValidationLayerUI;
import pixelitor.utils.TextFieldValidator;
import pixelitor.utils.ValidatedForm;

import javax.swing.*;
import javax.swing.plaf.LayerUI;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;

public class OutputSettingsPanel extends ValidatedForm implements TextFieldValidator {
    private JTextField nrSecondsTF = new JTextField("2", 3);
    private JTextField fpsTF = new JTextField("24", 3);
    private int nrFrames;
    private final JLabel nrFramesLabel;
    private double fps;
    private TweenWizard wizard;
    private final JComboBox<Interpolation> ipCB;
    private EnumComboBoxModel<TweenOutputType> model;
    private final JComboBox<TweenOutputType> outputTypeCB;
    private BrowseFilesSupport browseFilesSupport = new BrowseFilesSupport(FileChooser.getLastSaveDir().getAbsolutePath());
    private final JTextField fileNameTF;
    private String errorMessage;

    public OutputSettingsPanel(TweenWizard wizard) {
        super(new GridBagLayout());
        this.wizard = wizard;

        // A single TFValidationLayerUI for all the textfields.
        LayerUI<JTextField> tfLayerUI = new TFValidationLayerUI(this);

        //noinspection unchecked
        model = new EnumComboBoxModel(TweenOutputType.class);
        outputTypeCB = new JComboBox<>(model);
        outputTypeCB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                outputTypeChanged();
            }
        });
        outputTypeChanged(); // initial setup

        GridBagHelper.addLabelWithControl(this, "Output Type:", outputTypeCB, 0);

        GridBagHelper.addLabelWithControl(this, "Number of Seconds:",
                new JLayer<>(nrSecondsTF, tfLayerUI), 1);

        KeyAdapter keyAdapter = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent keyEvent) {
                updateCalculations(keyEvent);
            }
        };
        nrSecondsTF.addKeyListener(keyAdapter);

        GridBagHelper.addLabelWithControl(this, "Frames per Second:",
                new JLayer<>(fpsTF, tfLayerUI), 2);

        fpsTF.addKeyListener(keyAdapter);

        nrFramesLabel = new JLabel();
        updateCalculations(null);

        GridBagHelper.addLabelWithControl(this, "Number of Frames:", nrFramesLabel, 3);

        EnumComboBoxModel<Interpolation> ipCBM = new EnumComboBoxModel<>(Interpolation.class);
        ipCB = new JComboBox<>(ipCBM);

        GridBagHelper.addLabelWithControl(this, "Interpolation:", ipCB, 4);

        JPanel p = new JPanel(new FlowLayout());
        p.setBorder(BorderFactory.createTitledBorder("Output File/Folder"));
        fileNameTF = browseFilesSupport.getNameTF();
        p.add(new JLayer<>(fileNameTF, tfLayerUI));
        p.add(browseFilesSupport.getBrowseButton());
        GridBagHelper.addOnlyControlToRow(this, p, 5);

    }

    private void outputTypeChanged() {
        TweenOutputType selected = (TweenOutputType) outputTypeCB.getSelectedItem();
        if(selected.needsDirectory()) {
            browseFilesSupport.setSelectDirs(true);
            browseFilesSupport.setDialogTitle("Select Output Folder");
        } else {
            browseFilesSupport.setSelectDirs(false);
            browseFilesSupport.setDialogTitle("Select Output File");
        }
        if (fileNameTF != null) { // not the initial setup
            fileNameTF.repaint();
        }
    }

    private void updateCalculations(KeyEvent e) {
        try {
            double nrSeconds = Double.parseDouble(nrSecondsTF.getText().trim());
            fps = Double.parseDouble(fpsTF.getText().trim());
            nrFrames = (int) (nrSeconds * fps);
            nrFramesLabel.setText(String.valueOf(nrFrames));
        } catch (Exception ex) {
            nrFramesLabel.setText("??");
        }
    }

    public int getNumFrames() {
        return nrFrames;
    }

    public int getMillisBetweenFrames() {
        return (int) (1000.0 / fps);
    }

    public Interpolation getInterpolation() {
        return (Interpolation) ipCB.getSelectedItem();
    }

    public TweenOutputType getTweenOutputType() {
        return (TweenOutputType) outputTypeCB.getSelectedItem();
    }

    public File getOutput() {
        return browseFilesSupport.getSelectedFile();
    }

    @Override
    public boolean isDataValid() {
        return isValid(nrSecondsTF) && isValid(fpsTF) && isValid(fileNameTF);
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public boolean isValid(JTextField textField) {
        boolean valid = true;
        if (textField == nrSecondsTF || textField == fpsTF) {
            String text = textField.getText().trim();
            try {
                Double.parseDouble(text);
            } catch (NumberFormatException ex) {
                valid = false;
                errorMessage = text + " is not a valid number.";
            }
        } else if (textField == fileNameTF) {
            TweenOutputType outputType = (TweenOutputType) outputTypeCB.getSelectedItem();
            errorMessage = outputType.checkFile(new File(textField.getText().trim()));
            valid = (errorMessage == null);
        }
        return valid;
    }
}
