/**********************************************************************
 * $Source$
 * $Revision$
 * $Date$
 * $Author$
 *
 * Copyright (c) by Heiner Jostkleigrewe
 * This program is free software: you can redistribute it and/or modify it under the terms of the 
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without 
 *  even the implied warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See 
 *  the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If not, 
 * see <http://www.gnu.org/licenses/>.
 * 
 * heiner@jverein.de
 * www.jverein.de
 **********************************************************************/
package de.jost_net.JVerein.gui.view;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import de.jost_net.JVerein.gui.action.DokumentationAction;
import de.jost_net.JVerein.gui.control.MailControlPersonalbogen;
import de.jost_net.JVerein.gui.dialogs.MailEmpfaengerAuswahlDialog;
import de.jost_net.JVerein.gui.util.JameicaUtil;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.parts.Button;
import de.willuhn.jameica.gui.parts.ButtonArea;
import de.willuhn.util.ApplicationException;

public class MailPersonalbogenView extends AbstractView
{

  @Override
  public void bind() throws Exception
  {
    GUI.getView().setTitle("JVerein-Mail");

    final MailControlPersonalbogen control = new MailControlPersonalbogen(this);

    Composite comp = new Composite(this.getParent(), SWT.NONE);
    comp.setLayoutData(new GridData(GridData.FILL_BOTH));

    GridLayout layout = new GridLayout(2, false);
    comp.setLayout(layout);

    JameicaUtil.addLabel("Empfänger", comp, GridData.VERTICAL_ALIGN_BEGINNING);
    Composite comp2 = new Composite(comp, SWT.NONE);
    GridData gd2 = new GridData(GridData.FILL_HORIZONTAL);
    gd2.heightHint = 100;
    comp2.setLayoutData(gd2);
    GridLayout gl2 = new GridLayout();
    gl2.marginWidth = 0;
    comp2.setLayout(gl2);
    control.getEmpfaenger().paint(comp2);

    Composite comp3 = new Composite(comp, SWT.NONE);
    GridData gd3 = new GridData(GridData.HORIZONTAL_ALIGN_END);
    gd3.horizontalSpan = 2;
    comp3.setLayoutData(gd3);
    GridLayout gl3 = new GridLayout();
    gl3.marginWidth = 0;
    comp3.setLayout(gl3);
    Button add = new Button("Hinzufügen", new Action()
    {

      @Override
      public void handleAction(Object context) throws ApplicationException
      {
        MailEmpfaengerAuswahlDialog mead = new MailEmpfaengerAuswahlDialog(
            control, MailEmpfaengerAuswahlDialog.POSITION_CENTER);
        try
        {
          mead.open();
        }
        catch (Exception e)
        {
          throw new ApplicationException(e.getMessage());
        }
      }
    });
    add.paint(comp3);

    JameicaUtil.addLabel("Betreff", comp, GridData.VERTICAL_ALIGN_CENTER);
    control.getBetreff().paint(comp);
    JameicaUtil.addLabel("Text", comp, GridData.VERTICAL_ALIGN_BEGINNING);
    control.getTxt().paint(comp);

    JameicaUtil.addLabel("Anhang", comp, GridData.VERTICAL_ALIGN_BEGINNING);
    JameicaUtil.addLabel("Personalbogen", comp, GridData.VERTICAL_ALIGN_BEGINNING);

    ButtonArea buttons = new ButtonArea();
    buttons.addButton("Hilfe", new DokumentationAction(), DokumentationUtil.MAIL, false, "help-browser.png");
//    buttons.addButton(control.getMailSpeichernButton());
    buttons.addButton(control.getMailSendButton());
    buttons.paint(this.getParent());
  }

  @Override
  public String getHelp()
  {
    return "<form><p><span color=\"header\" font=\"header\">Mails</span></p>"
        + "<p><b>TIPP! </b>Mit einem Rechtsklick auf eine Mail-Adresse können alle Variable angezeigt werden.</p>"
        + "<p><b>TIPP! </b>Mit einem Rechtsklick auf eine Mail-Adresse kann eine Vorschau angezeigt werden.</p>"
        + "</form>";
  }

}
