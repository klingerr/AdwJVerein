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
package de.jost_net.JVerein.gui.menu;

import de.jost_net.JVerein.gui.action.MailAnhangAnzeigeAction;
import de.jost_net.JVerein.gui.action.MailAnhangDeleteAction;
import de.jost_net.JVerein.gui.action.MailAnhangSpeichernAction;
import de.jost_net.JVerein.gui.control.MailControl;
import de.willuhn.jameica.gui.parts.CheckedContextMenuItem;
import de.willuhn.jameica.gui.parts.CheckedSingleContextMenuItem;
import de.willuhn.jameica.gui.parts.ContextMenu;

/**
 * Kontext-Menu zu Mailanhängen.
 */
public class MailAnhangMenu extends ContextMenu
{

  /**
   * Erzeugt ein Kontext-Menu fuer die Liste von Mailanhängen.
   */
  public MailAnhangMenu() {
    this(null);
  }
  
  public MailAnhangMenu(MailControl control)
  {
    addItem(new CheckedContextMenuItem("speichern",
        new MailAnhangSpeichernAction(), "save.png"));
    addItem(new CheckedSingleContextMenuItem("anzeigen",
        new MailAnhangAnzeigeAction(), "show.png"));
    addItem(new CheckedSingleContextMenuItem("entfernen",
    	new MailAnhangDeleteAction(control), "user-trash.png"));
  }
  
}
