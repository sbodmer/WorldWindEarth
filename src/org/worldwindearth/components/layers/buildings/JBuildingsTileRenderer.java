/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.worldwindearth.components.layers.buildings;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 *
 * @author sbodmer
 */
public class JBuildingsTileRenderer extends javax.swing.JPanel implements ListCellRenderer {

    /**
     * Creates new form JBuildingTileRenderer
     */
    public JBuildingsTileRenderer() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        TF_Title = new javax.swing.JLabel();
        TF_Comments = new javax.swing.JLabel();
        PB_Progress = new javax.swing.JProgressBar();
        LB_Error = new javax.swing.JLabel();

        TF_Title.setText("...");

        TF_Comments.setText("...");

        PB_Progress.setStringPainted(true);

        LB_Error.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/worldwindearth/components/layers/buildings/Resources/Icons/16x16/error.png"))); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(TF_Title, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(TF_Comments, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(PB_Progress, javax.swing.GroupLayout.DEFAULT_SIZE, 392, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(LB_Error)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(TF_Title)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(TF_Comments)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(PB_Progress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(LB_Error))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus) {
        BuildingsTile t = (BuildingsTile) value;
        TF_Title.setText(t.level+"x"+t.x+"x"+t.y);
        TF_Comments.setText(t.getURL().toString());
        LB_Error.setVisible(t.hasError() != null);
        PB_Progress.setString(t.getProgress());
        if (t.isLoaded()) {
            PB_Progress.setVisible(false);
            
        } else if (t.getCurrentLoadedProgress() != -1) {
            PB_Progress.setValue((int) t.getCurrentLoadedProgress()/1024);
            PB_Progress.setValue((int) t.getTotalToLoad()/1024);
            
        } else {
            
        }
        if (t.hasError() != null) {
            LB_Error.setVisible(true);
            LB_Error.setToolTipText(t.hasError());
            PB_Progress.setString(t.hasError());
            
        } else {
            LB_Error.setVisible(false);
            LB_Error.setToolTipText(null);
            
        }
        
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            
        } else {
            setBackground(Color.WHITE);
        }
        return this;
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    protected javax.swing.JLabel LB_Error;
    protected javax.swing.JProgressBar PB_Progress;
    protected javax.swing.JLabel TF_Comments;
    protected javax.swing.JLabel TF_Title;
    // End of variables declaration//GEN-END:variables
}
