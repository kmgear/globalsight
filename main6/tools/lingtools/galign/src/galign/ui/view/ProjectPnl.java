/*
 * ProjectPnl.java
 *
 * Created on July 29, 2004, 12:29 AM
 */

package galign.ui.view;

import galign.Setup;

/**
 *
 * @author  cvdlaan
 */
public class ProjectPnl extends javax.swing.JPanel
{
    
    /** Creates new form ProjectPnl */
    public ProjectPnl ()
    {
        initComponents ();
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        lb_projectName = new javax.swing.JLabel();
        lb_sourceLocale = new javax.swing.JLabel();
        lb_targetLocale = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();

        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel1.setText(Setup.getLabel("label.name"));
        add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(90, 40, -1, -1));

        jLabel2.setText(Setup.getLabel("label.sourceLocale"));
        add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 60, -1, -1));

        jLabel3.setText(Setup.getLabel("label.targetLocale"));
        add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 80, -1, -1));

        lb_projectName.setText(" ");
        add(lb_projectName, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 40, -1, -1));

        lb_sourceLocale.setText(" ");
        add(lb_sourceLocale, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 60, -1, -1));

        lb_targetLocale.setText(" ");
        add(lb_targetLocale, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 80, -1, -1));

        String str = Setup.getString("label.projectProperties");
        jLabel4.setText("<HTML><B>" + str + "</B></HTML>");
        add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 20, 240, -1));

    }//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JLabel jLabel1;
    public javax.swing.JLabel jLabel2;
    public javax.swing.JLabel jLabel3;
    public javax.swing.JLabel jLabel4;
    public javax.swing.JLabel lb_projectName;
    public javax.swing.JLabel lb_sourceLocale;
    public javax.swing.JLabel lb_targetLocale;
    // End of variables declaration//GEN-END:variables
    
}
