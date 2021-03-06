/**********************************************************************
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
package de.jost_net.JVerein.gui.boxes;

import java.rmi.RemoteException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import de.jost_net.JVerein.gui.action.SEPAKonvertierungAction;
import de.jost_net.JVerein.gui.control.SEPAKonvertierungControl;
import de.willuhn.datasource.rmi.DBIterator;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.boxes.AbstractBox;
import de.willuhn.jameica.gui.parts.Button;
import de.willuhn.jameica.gui.parts.ButtonArea;
import de.willuhn.jameica.gui.util.Font;
import de.willuhn.jameica.gui.util.SWTUtil;
import de.willuhn.jameica.system.Application;
import de.willuhn.jameica.system.Platform;
import de.willuhn.logging.Logger;

/**
 * Seite fuer die SEPA-Konvertierung.
 */
public class SEPAKonverter extends AbstractBox
{

  private boolean aktiv = false;

  public SEPAKonverter()
  {
    try
    {
      DBIterator it = SEPAKonvertierungControl.getMitglieder();
      it.addFilter("iban is null and length(blz) > 0");
      if (it.size() > 0)
      {
        aktiv = true;
      }
    }
    catch (RemoteException e)
    {
      Logger.error("Fehler", e);
    }

  }

  @Override
  public boolean isActive()
  {
    return aktiv;
  }

  @Override
  public boolean getDefaultEnabled()
  {
    return aktiv;
  }

  @Override
  public int getDefaultIndex()
  {
    return 0;
  }

  @Override
  public String getName()
  {
    return "JVerein: SEPA-Konvertierung";
  }

  @Override
  public boolean isEnabled()
  {
    return aktiv;
  }

  @Override
  public void paint(Composite parent) throws RemoteException
  {
    // Wir unterscheiden hier beim Layout nach Windows/OSX und Rest.
    // Unter Windows und OSX sieht es ohne Rahmen und ohne Hintergrund besser
    // aus
    org.eclipse.swt.graphics.Color bg = null;
    int border = SWT.NONE;

    int os = Application.getPlatform().getOS();
    if (os != Platform.OS_WINDOWS && os != Platform.OS_WINDOWS_64
        && os != Platform.OS_MAC)
    {
      bg = GUI.getDisplay().getSystemColor(SWT.COLOR_WHITE);
      border = SWT.BORDER;
    }

    // 2-spaltige Anzeige. Links das Icon, rechts Text und Buttons
    Composite comp = new Composite(parent, border);
    comp.setBackground(bg);
    comp.setBackgroundMode(SWT.INHERIT_FORCE);
    comp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    comp.setLayout(new GridLayout(2, false));

    // Linke Spalte mit dem Icon
    {
      GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING
          | GridData.VERTICAL_ALIGN_BEGINNING);
      gd.verticalSpan = 3;
      Label icon = new Label(comp, SWT.NONE);
      icon.setBackground(bg);
      icon.setLayoutData(gd);
      icon.setImage(SWTUtil.getImage("jverein-icon-64x64.png"));
    }

    // Ueberschrift
    {
      Label title = new Label(comp, SWT.NONE);
      title.setBackground(bg);
      title.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      title.setFont(Font.H2.getSWTFont());
      title.setText("SEPA-Konvertierung.");
    }

    // Text
    {
      Label desc = new Label(comp, SWT.WRAP);
      desc.setBackground(bg);
      desc.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      desc.setText("Konvertierung der Bankverbindung bei den Einstellungen, Mitgliedern und Kursteilnehmern "
          + "von BLZ und Konto zu BIC und IBAN");
    }

    ButtonArea buttons = new ButtonArea();
    buttons.addButton(getButton());
    buttons.paint(parent);
  }

  private Button getButton()
  {
    Button b = new Button("starten", new SEPAKonvertierungAction(), null, true,
        "document-save.png");
    return b;
  }

  @Override
  public int getHeight()
  {
    return 140;
  }

}
