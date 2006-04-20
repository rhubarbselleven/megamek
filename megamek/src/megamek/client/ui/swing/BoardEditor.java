/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 * 
 *  This program is free software; you can redistribute it and/or modify it 
 *  under the terms of the GNU General Public License as published by the Free 
 *  Software Foundation; either version 2 of the License, or (at your option) 
 *  any later version.
 * 
 *  This program is distributed in the hope that it will be useful, but 
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 *  for more details.
 */
package megamek.client.ui.swing;

import keypoint.PngEncoder;
import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListenerAdapter;
import megamek.common.Coords;
import megamek.common.Game;
import megamek.common.Hex;
import megamek.common.IBoard;
import megamek.common.IHex;
import megamek.common.ITerrain;
import megamek.common.MapSettings;
import megamek.common.Terrains;
import megamek.common.util.BoardUtilities;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.List;
import java.awt.Scrollbar;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BoardEditor extends Container implements ItemListener,
        ActionListener, DocumentListener, IMapSettingsObserver {
    private JFrame frame = new JFrame();
    private Game game = new Game();
    private IBoard board = game.getBoard();
    private BoardView1 bv;
    private CommonMenuBar menuBar = new CommonMenuBar();
    private CommonAboutDialog about = null;
    private CommonHelpDialog help = null;
    private CommonSettingsDialog setdlg = null;
    private IHex curHex = new Hex();
    private String curpath, curfile, curfileImage;
    // buttons and labels and such:
    private HexCanvas canHex;
    private JLabel labElev;
    private JTextField texElev;
    private JButton butElevUp;
    private JButton butElevDown;
    private JLabel labTerrain;
    private List lisTerrain;
    private JButton butDelTerrain;
    private JPanel panTerrainType;
    private Choice choTerrainType;
    private JTextField texTerrainLevel;
    private JPanel panTerrExits;
    private JCheckBox cheTerrExitSpecified;
    private JTextField texTerrExits;
    private JButton butTerrExits;
    private JPanel panRoads;
    private JCheckBox cheRoadsAutoExit;
    private JLabel labTheme;
    private JTextField texTheme;
    private JButton butAddTerrain;
    private JLabel blankL;
    private JLabel labBoard;
    private JPanel panButtons;
    private JButton butBoardNew, butBoardLoad;
    private JButton butBoardSave, butBoardSaveAs;
    private JButton butBoardSaveAsImage;
    private JButton butMiniMap;
    private JDialog minimapW;
    private MiniMap minimap;
    private MapSettings mapSettings = new MapSettings();

    /**
     * Creates and lays out a new Board Editor frame.
     */
    public BoardEditor() {
        try {
            bv = new BoardView1(game, frame);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, Messages.getString("BoardEditor.CouldntInitialize") + e, Messages.getString("BoardEditor.FatalError"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
            frame.dispose();
        }
        bv.addBoardViewListener(new BoardViewListenerAdapter() {
            public void hexMoused(BoardViewEvent b) {
                bv.cursor(b.getCoords());
                if ((b.getModifiers() & InputEvent.ALT_MASK) != 0) {
                    setCurrentHex(board.getHex(b.getCoords()));
                }
                if ((b.getModifiers() & InputEvent.CTRL_MASK) != 0) {
                    if (!board.getHex(b.getCoords()).equals(curHex)) {
                        paintHex(b.getCoords());
                    }
                }
            }
        });
        bv.setUseLOSTool(false);
        setupEditorPanel();
        setupFrame();
        frame.setVisible(true);
        if (GUIPreferences.getInstance().getNagForMapEdReadme()) {
            String title = Messages.getString("BoardEditor.readme.title"); //$NON-NLS-1$
            String body = Messages.getString("BoardEditor.readme.message"); //$NON-NLS-1$
            ConfirmDialog confirm = new ConfirmDialog(frame, title, body, true);
            confirm.setVisible(true);
            if (!confirm.getShowAgain()) {
                GUIPreferences.getInstance().setNagForMapEdReadme(false);
            }
            if (confirm.getAnswer()) {
                showHelp();
            }
        }
    }

    /**
     * Sets up the frame that will display the editor.
     */
    private void setupFrame() {
        frame.setTitle(Messages.getString("BoardEditor.title")); //$NON-NLS-1$
        frame.getContentPane().setLayout(new BorderLayout());

        // Create a scroll bars to surround the board view.
        JPanel scrollPane = new JPanel();
        scrollPane.setLayout(new BorderLayout());
        Scrollbar vertical = new Scrollbar(Scrollbar.VERTICAL);
        Scrollbar horizontal = new Scrollbar(Scrollbar.HORIZONTAL);
        scrollPane.add(bv, BorderLayout.CENTER);
        scrollPane.add(vertical, BorderLayout.EAST);
        scrollPane.add(horizontal, BorderLayout.SOUTH);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

        // Assign the scrollbars to the board viewer.
        bv.setScrollbars(vertical, horizontal);
        frame.getContentPane().add(this, BorderLayout.EAST);
        menuBar.addActionListener(this);
        frame.setMenuBar(menuBar);
        frame.setBackground(SystemColor.menu);
        frame.setForeground(SystemColor.menuText);
        if (GUIPreferences.getInstance().getWindowSizeHeight() != 0) {
            frame.setLocation(GUIPreferences.getInstance().getWindowPosX(), GUIPreferences.getInstance().getWindowPosY());
            frame.setSize(GUIPreferences.getInstance().getWindowSizeWidth(), GUIPreferences.getInstance().getWindowSizeHeight());
        } else {
            frame.setSize(800, 600);
        }

        // Give the scrollbars large initial values.
        horizontal.setVisibleAmount(frame.getSize().width);
        vertical.setVisibleAmount(frame.getSize().height);

        // when frame is closing, just hide it
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                frame.setVisible(false);
                setMapVisible(false);
            }
        });
    }

    /**
     * Sets up the editor panel, which goes on the right of the map and has
     * controls for editing the current square.
     */
    private void setupEditorPanel() {
        canHex = new HexCanvas();
        labElev = new JLabel(Messages.getString("BoardEditor.labElev"), JLabel.RIGHT); //$NON-NLS-1$
        texElev = new JTextField("0", 1); //$NON-NLS-1$
        texElev.addActionListener(this);
        texElev.getDocument().addDocumentListener(this);
        butElevUp = new JButton(Messages.getString("BoardEditor.butElevUp")); //$NON-NLS-1$
        butElevUp.addActionListener(this);
        butElevDown = new JButton(Messages.getString("BoardEditor.butElevDown")); //$NON-NLS-1$
        butElevDown.addActionListener(this);
        labTerrain = new JLabel(Messages.getString("BoardEditor.labTerrain"), JLabel.LEFT); //$NON-NLS-1$
        lisTerrain = new List(6);
        lisTerrain.addItemListener(this);
        refreshTerrainList();
        butDelTerrain = new JButton(Messages.getString("BoardEditor.butDelTerrain")); //$NON-NLS-1$
        butDelTerrain.addActionListener(this);
        choTerrainType = new Choice();
        for (int i = 1; i < Terrains.SIZE; i++) {
            choTerrainType.add(Terrains.getName(i));
        }
        texTerrainLevel = new JTextField("0", 1); //$NON-NLS-1$
        butAddTerrain = new JButton(Messages.getString("BoardEditor.butAddTerrain")); //$NON-NLS-1$
        butAddTerrain.addActionListener(this);
        butMiniMap = new JButton(Messages.getString("BoardEditor.butMiniMap")); //$NON-NLS-1$
        butMiniMap.setActionCommand("viewMiniMap"); //$NON-NLS-1$
        butMiniMap.addActionListener(this);
        panTerrainType = new JPanel(new BorderLayout());
        panTerrainType.add(choTerrainType, BorderLayout.WEST);
        panTerrainType.add(texTerrainLevel, BorderLayout.CENTER);
        cheTerrExitSpecified = new JCheckBox(Messages.getString("BoardEditor.cheTerrExitSpecified")); //$NON-NLS-1$
        butTerrExits = new JButton(Messages.getString("BoardEditor.butTerrExits")); //$NON-NLS-1$
        texTerrExits = new JTextField("0", 1); //$NON-NLS-1$
        butTerrExits.addActionListener(this);
        panTerrExits = new JPanel(new FlowLayout());
        panTerrExits.add(cheTerrExitSpecified);
        panTerrExits.add(butTerrExits);
        panTerrExits.add(texTerrExits);
        panRoads = new JPanel(new FlowLayout());
        cheRoadsAutoExit = new JCheckBox(Messages.getString("BoardEditor.cheRoadsAutoExit")); //$NON-NLS-1$
        cheRoadsAutoExit.addItemListener(this);
        panRoads.add(cheRoadsAutoExit);
        labTheme = new JLabel(Messages.getString("BoardEditor.labTheme"), JLabel.LEFT); //$NON-NLS-1$
        texTheme = new JTextField("", 15); //$NON-NLS-1$
        texTheme.getDocument().addDocumentListener(this);
        labBoard = new JLabel(Messages.getString("BoardEditor.labBoard"), JLabel.LEFT); //$NON-NLS-1$
        butBoardNew = new JButton(Messages.getString("BoardEditor.butBoardNew")); //$NON-NLS-1$
        butBoardNew.setActionCommand("fileBoardNew"); //$NON-NLS-1$
        butBoardNew.addActionListener(this);
        butBoardLoad = new JButton(Messages.getString("BoardEditor.butBoardLoad")); //$NON-NLS-1$
        butBoardLoad.setActionCommand("fileBoardOpen"); //$NON-NLS-1$
        butBoardLoad.addActionListener(this);
        butBoardSave = new JButton(Messages.getString("BoardEditor.butBoardSave")); //$NON-NLS-1$
        butBoardSave.setActionCommand("fileBoardSave"); //$NON-NLS-1$
        butBoardSave.addActionListener(this);
        butBoardSaveAs = new JButton(Messages.getString("BoardEditor.butBoardSaveAs")); //$NON-NLS-1$
        butBoardSaveAs.setActionCommand("fileBoardSaveAs"); //$NON-NLS-1$
        butBoardSaveAs.addActionListener(this);
        butBoardSaveAsImage = new JButton(Messages.getString("BoardEditor.butBoardSaveAsImage")); //$NON-NLS-1$
        butBoardSaveAsImage.setActionCommand("fileBoardSaveAsImage"); //$NON-NLS-1$
        butBoardSaveAsImage.addActionListener(this);
        panButtons = new JPanel(new GridLayout(3, 2, 2, 2));
        panButtons.add(labBoard);
        panButtons.add(butBoardNew);
        panButtons.add(butBoardLoad);
        panButtons.add(butBoardSave);
        panButtons.add(butBoardSaveAs);
        panButtons.add(butBoardSaveAsImage);
        blankL = new JLabel("", JLabel.CENTER); //$NON-NLS-1$
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(4, 4, 1, 1);
        c.gridwidth = GridBagConstraints.REMAINDER;
        addBag(canHex, gridbag, c);
        c.gridwidth = 1;
        addBag(labElev, gridbag, c);
        addBag(butElevUp, gridbag, c);
        addBag(butElevDown, gridbag, c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        addBag(texElev, gridbag, c);
        addBag(labTerrain, gridbag, c);
        addBag(lisTerrain, gridbag, c);
        addBag(butDelTerrain, gridbag, c);
        addBag(panTerrainType, gridbag, c);
        addBag(panTerrExits, gridbag, c);
        addBag(panRoads, gridbag, c);
        addBag(labTheme, gridbag, c);
        addBag(texTheme, gridbag, c);
        addBag(butAddTerrain, gridbag, c);
        addBag(butMiniMap, gridbag, c);
        c.weightx = 1.0;
        c.weighty = 1.0;
        addBag(blankL, gridbag, c);
        c.weightx = 1.0;
        c.weighty = 0.0;
        //        addBag(labBoard, gridbag, c);
        addBag(panButtons, gridbag, c);
        minimapW = new JDialog(frame, Messages.getString("BoardEditor.minimapW"), false); //$NON-NLS-1$
        minimapW.setLocation(GUIPreferences.getInstance().getMinimapPosX(), GUIPreferences.getInstance().getMinimapPosY());
        try {
            minimap = new MiniMap(minimapW, game, bv);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, Messages.getString("BoardEditor.CouldNotInitialiseMinimap") + e, Messages.getString("BoardEditor.FatalError"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
            frame.dispose();
        }
        minimapW.add(minimap);
        setMapVisible(true);
    }

    private void addBag(Component comp, GridBagLayout gridbag, GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
    }

    /**
     * Apply the current Hex to the Board at the specified
     * location.
     */
    public void paintHex(Coords c) {
        board.setHex(c, curHex.duplicate());
    }

    /**
     * Sets the current hex
     *
     * @param hex hex to set.
     */
    public void setCurrentHex(IHex hex) {
        curHex = hex.duplicate();
        texElev.setText(Integer.toString(curHex.getElevation()));
        refreshTerrainList();
        if (lisTerrain.getItemCount() > 0) {
            lisTerrain.select(0);
            refreshTerrainFromList();
        }
        texTheme.setText(curHex.getTheme());
        repaint();
        repaintWorkingHex();
    }

    private void repaintWorkingHex() {
        if (curHex != null) {
            TilesetManager tm = bv.getTilesetManager();
            tm.clearHex(curHex);
        }
        canHex.repaint();
    }

    /**
     * Refreshes the terrain list to match the current hex
     */
    public void refreshTerrainList() {
        lisTerrain.removeAll();
        for (int i = 0; i < Terrains.SIZE; i++) {
            ITerrain terrain = curHex.getTerrain(i);
            if (terrain != null) {
                lisTerrain.add(terrain.toString());
            }
        }
    }

    /**
     * Returns a new instance of the terrain that is currently entered in the
     * terrain input fields
     */
    private ITerrain enteredTerrain() {
        int type = Terrains.getType(choTerrainType.getSelectedItem());
        int level = Integer.parseInt(texTerrainLevel.getText());
        boolean exitsSpecified = cheTerrExitSpecified.isSelected();
        int exits = Integer.parseInt(texTerrExits.getText());
        return Terrains.getTerrainFactory().createTerrain(type, level, exitsSpecified, exits);
    }

    /**
     * Add or set the terrain to the list based on the fields.
     */
    private void addSetTerrain() {
        ITerrain toAdd = enteredTerrain();
        curHex.addTerrain(toAdd);
        refreshTerrainList();
        repaintWorkingHex();
    }

    /**
     * Set all the appropriate terrain field to match the currently selected
     * terrain in the list.
     */
    private void refreshTerrainFromList() {
        ITerrain terrain = Terrains.getTerrainFactory().createTerrain(lisTerrain.getSelectedItem());
        terrain = curHex.getTerrain(terrain.getType());
        choTerrainType.select(Terrains.getName(terrain.getType()));
        texTerrainLevel.setText(Integer.toString(terrain.getLevel()));
        cheTerrExitSpecified.setSelected(terrain.hasExitsSpecified());
        texTerrExits.setText(Integer.toString(terrain.getExits()));
    }

    /**
     * Initialize a new data set in the current board.
     * <p/>
     * If hexes are loaded, brings up a dialog box requesting
     * width and height and default hex.  If height and width
     * are valid, creates new board data and fills it with the
     * selected hex.
     */
    public void boardNewXX() {
        // display new board dialog
        BoardNewDialog bnd = new BoardNewDialog(frame);
        bnd.setLocation(frame.getLocation().x + 150, frame.getLocation().y + 100);
        bnd.setVisible(true);
        if (bnd.getX() > 0 || bnd.getY() > 0) {
            IHex[] newHexes = new IHex[bnd.getX() * bnd.getY()];
            for (int i = 0; i < newHexes.length; i++) {
                newHexes[i] = new Hex();
            }
            board.newData(bnd.getX(), bnd.getY(), newHexes);
            curpath = null;
            curfile = null;
            frame.setTitle(Messages.getString("BoardEditor.title")); //$NON-NLS-1$
            menuBar.setBoard(true);
        }
    }

    public void boardNew() {
        RandomMapDialog rmd = new RandomMapDialog(frame, this, mapSettings);
        rmd.setVisible(true);
        board = BoardUtilities.generateRandom(mapSettings);
        game.setBoard(board);
        curpath = null;
        curfile = null;
        frame.setTitle(Messages.getString("BoardEditor.title")); //$NON-NLS-1$
        menuBar.setBoard(true);
    }

    public void updateMapSettings(MapSettings newSettings) {
        mapSettings = newSettings;
    }

    public void boardLoad() {
        FileDialog fd = new FileDialog(frame, Messages.getString("BoardEditor.loadBoard"), FileDialog.LOAD); //$NON-NLS-1$
        fd.setDirectory("data" + File.separator + "boards"); //$NON-NLS-1$ //$NON-NLS-2$
        fd.setLocation(frame.getLocation().x + 150, frame.getLocation().y + 100);
        fd.setVisible(true);
        if (fd.getFile() == null) {
            // I want a file, y'know!
            return;
        }
        curpath = fd.getDirectory();
        curfile = fd.getFile();
        // load!
        try {
            InputStream is = new FileInputStream(new File(curpath, curfile));
            // tell the board to load!
            board.load(is);
            // okay, done!
            is.close();
            menuBar.setBoard(true);
        } catch (IOException ex) {
            System.err.println("error opening file to save!"); //$NON-NLS-1$
            System.err.println(ex);
        }
        frame.setTitle(Messages.getString("BoardEditor.title0") + curfile); //$NON-NLS-1$
        cheRoadsAutoExit.setSelected(board.getRoadsAutoExit());
        refreshTerrainList();
    }

    /**
     * Checks to see if there is already a path and name
     * stored; if not, calls "save as"; otherwise, saves
     * the board to the specified file.
     */
    public void boardSave() {
        if (curfile == null) {
            boardSaveAs();
            return;
        }
        // save!
        try {
            OutputStream os = new FileOutputStream(new File(curpath, curfile));
            // tell the board to save!
            board.save(os);
            // okay, done!
            os.close();
        } catch (IOException ex) {
            System.err.println("error opening file to save!"); //$NON-NLS-1$
            System.err.println(ex);
        }
    }

    /**
     * Saves the board in PNG image format.
     */
    public void boardSaveImage() {
        if (curfileImage == null) {
            boardSaveAsImage();
            return;
        }
        JDialog waitD = new JDialog(frame, Messages.getString("BoardEditor.waitDialog.title")); //$NON-NLS-1$
        waitD.add(new JLabel(Messages.getString("BoardEditor.waitDialog.message"))); //$NON-NLS-1$
        waitD.setSize(250, 130);
        // move to middle of screen
        waitD.setLocation(frame.getSize().width / 2 - waitD.getSize().width / 2,
                frame.getSize().height / 2 - waitD.getSize().height / 2);
        waitD.setVisible(true);
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        waitD.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        // save!
        int filter = 0; //0 - no filter; 1 - sub; 2 - up
        int compressionLevel = 9; // 0 to 9 with 0 being no compression
        PngEncoder png = new PngEncoder(bv.getEntireBoardImage(),
                PngEncoder.NO_ALPHA,
                filter,
                compressionLevel);
        try {
            FileOutputStream outfile = new FileOutputStream(curfileImage);
            byte[] pngbytes;
            pngbytes = png.pngEncode();
            if (pngbytes == null) {
                System.out.println("Failed to save board as image:Null image"); //$NON-NLS-1$
            } else {
                outfile.write(pngbytes);
            }
            outfile.flush();
            outfile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        waitD.setVisible(false);
        frame.setCursor(Cursor.getDefaultCursor());
    }

    /**
     * Opens a file dialog box to select a file to save as;
     * saves the board to the file.
     */
    public void boardSaveAs() {
        FileDialog fd = new FileDialog(frame, Messages.getString("BoardEditor.saveBoardAs"), FileDialog.SAVE); //$NON-NLS-1$
        fd.setDirectory("data" + File.separator + "boards"); //$NON-NLS-1$ //$NON-NLS-2$
        fd.setLocation(frame.getLocation().x + 150, frame.getLocation().y + 100);
        fd.setVisible(true);
        if (fd.getFile() == null) {
            // I want a file, y'know!
            return;
        }
        curpath = fd.getDirectory();
        curfile = fd.getFile();

        // make sure the file ends in board
        if (!curfile.toLowerCase().endsWith(".board")) { //$NON-NLS-1$
            curfile += ".board"; //$NON-NLS-1$
        }
        frame.setTitle(Messages.getString("BoardEditor.title0") + curfile); //$NON-NLS-1$
        boardSave();
    }

    /**
     * Opens a file dialog box to select a file to save as;
     * saves the board to the file as an image.  Useful
     * for printing boards.
     */
    public void boardSaveAsImage() {
        FileDialog fd = new FileDialog(frame, Messages.getString("BoardEditor.saveAsImage"), FileDialog.SAVE); //$NON-NLS-1$
        //        fd.setDirectory("data" + File.separator + "boards");
        fd.setLocation(frame.getLocation().x + 150, frame.getLocation().y + 100);

        // Add a filter for PNG files
        fd.setFilenameFilter(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (null != name && name.endsWith(".png")); //$NON-NLS-1$
            }
        });

        // use base directory by default
        fd.setDirectory("."); //$NON-NLS-1$

        // Default to the board's name (if it has one).
        String fileName = null;
        if (null != curfile && curfile.length() > 0) {
            fileName = curfile.toUpperCase();
            if (fileName.endsWith(".BOARD")) { //$NON-NLS-1$
                int length = fileName.length();
                fileName = fileName.substring(0, length - 6);
            }
            fileName = fileName.toLowerCase() + ".png"; //$NON-NLS-1$
            fd.setFile(fileName);
        }

        // Open the dialog and wait for it's return.
        fd.setVisible(true);
        if (fd.getFile() == null) {
            // I want a file, y'know!
            return;
        }
        curpath = fd.getDirectory();
        curfileImage = fd.getFile();

        // make sure the file ends in board
        if (!curfileImage.toLowerCase().endsWith(".png")) { //$NON-NLS-1$
            curfileImage += ".png"; //$NON-NLS-1$
        }
        frame.setTitle(Messages.getString("BoardEditor.title0") + curfileImage); //$NON-NLS-1$
        boardSaveImage();
    }

    //
    // ItemListener
    //
    public void itemStateChanged(ItemEvent ie) {
        if (ie.getSource().equals(lisTerrain)) {
            refreshTerrainFromList();
        } else if (ie.getSource().equals(cheRoadsAutoExit)) {
            // Set the new value for the option, and refrest the board.
            board.setRoadsAutoExit(cheRoadsAutoExit.isSelected());
            bv.updateBoard();
            repaintWorkingHex();
        }
    }

    //
    // TextListener
    //
    public void changedUpdate(DocumentEvent te) {
        if (te.getDocument().equals(texElev.getDocument())) {
            int value;
            try {
                value = Integer.parseInt(texElev.getText());
            } catch (NumberFormatException ex) {
                return;
            }
            if (value != curHex.getElevation()) {
                curHex.setElevation(value);
                repaintWorkingHex();
            }
        } else if (te.getDocument().equals(texTheme.getDocument())) {
            curHex.setTheme(texTheme.getText());
            repaintWorkingHex();
        }
    }

    /**
     * Called when the user selects the "Help->About" menu item.
     */
    private void showAbout() {
        // Do we need to create the "about" dialog?
        if (about == null) {
            about = new CommonAboutDialog(frame);
        }

        // Show the about dialog.
        about.setVisible(true);
    }

    /**
     * Called when the user selects the "Help->Contents" menu item.
     */
    private void showHelp() {
        // Do we need to create the "help" dialog?
        if (help == null) {
            File helpfile = new File("docs", "editor-readme.txt"); //$NON-NLS-1$
            help = new CommonHelpDialog(frame, helpfile);
        }

        // Show the help dialog.
        help.setVisible(true);
    }

    /**
     * Called when the user selects the "View->Client Settings" menu item.
     */
    private void showSettings() {
        // Do we need to create the "settings" dialog?
        if (setdlg == null) {
            setdlg = new CommonSettingsDialog(frame);
        }

        // Show the settings dialog.
        setdlg.setVisible(true);
    }

    //
    // ActionListener
    //
    public void actionPerformed(ActionEvent ae) {
        if ("fileBoardNew".equalsIgnoreCase(ae.getActionCommand())) { //$NON-NLS-1$
            boardNew();
        } else if ("fileBoardOpen".equalsIgnoreCase(ae.getActionCommand())) { //$NON-NLS-1$
            boardLoad();
        } else if ("fileBoardSave".equalsIgnoreCase(ae.getActionCommand())) { //$NON-NLS-1$
            boardSave();
        } else if ("fileBoardSaveAs".equalsIgnoreCase(ae.getActionCommand())) { //$NON-NLS-1$
            boardSaveAs();
        } else if ("fileBoardSaveAsImage".equalsIgnoreCase(ae.getActionCommand())) { //$NON-NLS-1$
            boardSaveAsImage();
        } else if (ae.getSource().equals(butDelTerrain) && lisTerrain.getSelectedItem() != null) {
            ITerrain toRemove = Terrains.getTerrainFactory().createTerrain(lisTerrain.getSelectedItem());
            curHex.removeTerrain(toRemove.getType());
            refreshTerrainList();
            repaintWorkingHex();
        } else if (ae.getSource().equals(butAddTerrain)) {
            addSetTerrain();
        } else if (ae.getSource().equals(butElevUp) && curHex.getElevation() < 9) {
            curHex.setElevation(curHex.getElevation() + 1);
            texElev.setText(Integer.toString(curHex.getElevation()));
            repaintWorkingHex();
        } else if (ae.getSource().equals(butElevDown) && curHex.getElevation() > -5) {
            curHex.setElevation(curHex.getElevation() - 1);
            texElev.setText(Integer.toString(curHex.getElevation()));
            repaintWorkingHex();
        } else if (ae.getSource().equals(butTerrExits)) {
            ExitsDialog ed = new ExitsDialog(frame);
            cheTerrExitSpecified.setSelected(true);
            ed.setExits(Integer.parseInt(texTerrExits.getText()));
            ed.setVisible(true);
            texTerrExits.setText(Integer.toString(ed.getExits()));
            addSetTerrain();
        } else if ("viewMiniMap".equalsIgnoreCase(ae.getActionCommand())) { //$NON-NLS-1$
            toggleMap();
        } else if (ae.getActionCommand().equals(ClientGUI.VIEW_ZOOM_IN)) {
            bv.zoomIn();
        } else if (ae.getActionCommand().equals(ClientGUI.VIEW_ZOOM_OUT)) {
            bv.zoomOut();
        } else if ("helpAbout".equalsIgnoreCase(ae.getActionCommand())) { //$NON-NLS-1$
            showAbout();
        } else if ("helpContents".equalsIgnoreCase(ae.getActionCommand())) { //$NON-NLS-1$
            showHelp();
        } else if ("viewClientSettings".equalsIgnoreCase(ae.getActionCommand())) { //$NON-NLS-1$
            showSettings();
        }
    }

    public void insertUpdate(DocumentEvent event) {
        changedUpdate(event);
    }

    public void removeUpdate(DocumentEvent event) {
        changedUpdate(event);
    }

    /**
     * Displays the currently selected hex picture, in
     * component form
     */
    private class HexCanvas extends Canvas {
        HexCanvas() {
            setSize(72, 72);
        }

        public void paint(Graphics g) {
            update(g);
        }

        public void update(Graphics g) {
            if (curHex != null) {
                TilesetManager tm = bv.getTilesetManager();
                g.drawImage(tm.baseFor(curHex), 0, 0, this);
                g.setColor(getForeground());
                if (tm.supersFor(curHex) != null) {
                    for (final Object newVar : tm.supersFor(curHex)) {
                        g.drawImage((Image) newVar, 0, 0, this);
                        g.drawString(Messages.getString("BoardEditor.SUPER"), 0, 10); //$NON-NLS-1$
                    }
                }
                g.setFont(new Font("SansSerif", Font.PLAIN, 9)); //$NON-NLS-1$
                g.drawString(Messages.getString("BoardEditor.LEVEL") + curHex.getElevation(), 24, 70); //$NON-NLS-1$
            } else {
                g.clearRect(0, 0, 72, 72);
            }
        }
    }

    /**
     * @return the frame this is displayed in
     */
    public JFrame getFrame() {
        return frame;
    }

    /**
     * Toggles the minimap window
     * Also, toggles the minimap enabled setting
     */
    public void toggleMap() {
        setMapVisible(!minimapW.isVisible());
    }

    public void setMapVisible(boolean visible) {
        minimapW.setVisible(visible);
    }
}

/**
 * a quick class for the new map diaglogue box
 */
class BoardNewDialog extends JDialog implements ActionListener {
    public int xvalue, yvalue;
    protected JLabel labWidth, labHeight;
    protected JTextField texWidth, texHeight;
    protected JButton butOkay, butCancel;

    BoardNewDialog(JFrame frame) {
        super(frame, Messages.getString("BoardEditor.SetDimentions"), true); //$NON-NLS-1$
        xvalue = 0;
        yvalue = 0;
        labWidth = new JLabel(Messages.getString("BoardEditor.labWidth"), JLabel.RIGHT); //$NON-NLS-1$
        labHeight = new JLabel(Messages.getString("BoardEditor.labHeight"), JLabel.RIGHT); //$NON-NLS-1$
        texWidth = new JTextField("16", 2); //$NON-NLS-1$
        texHeight = new JTextField("17", 2); //$NON-NLS-1$
        butOkay = new JButton(Messages.getString("Okay")); //$NON-NLS-1$
        butOkay.setActionCommand("done"); //$NON-NLS-1$
        butOkay.addActionListener(this);
        butOkay.setSize(80, 24);
        butCancel = new JButton(Messages.getString("Cancel")); //$NON-NLS-1$
        butCancel.setActionCommand("cancel"); //$NON-NLS-1$
        butCancel.addActionListener(this);
        butCancel.setSize(80, 24);
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 1, 1);
        gridbag.setConstraints(labWidth, c);
        add(labWidth);
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(texWidth, c);
        add(texWidth);
        c.gridwidth = GridBagConstraints.RELATIVE;
        gridbag.setConstraints(labHeight, c);
        add(labHeight);
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(texHeight, c);
        add(texHeight);
        c.ipadx = 20;
        c.ipady = 5;
        c.gridwidth = GridBagConstraints.RELATIVE;
        gridbag.setConstraints(butOkay, c);
        add(butOkay);
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(butCancel, c);
        add(butCancel);
        pack();
        setResizable(false);
        setLocation(frame.getLocation().x + frame.getSize().width / 2 - getSize().width / 2,
                frame.getLocation().y + frame.getSize().height / 2 - getSize().height / 2);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(butOkay)) {
            try {
                xvalue = Integer.decode(texWidth.getText()).intValue();
                yvalue = Integer.decode(texHeight.getText()).intValue();
            } catch (NumberFormatException ex) {
                System.err.println(ex.getMessage());
            }
            setVisible(false);
        } else if (e.getSource().equals(butCancel)) {
            setVisible(false);
        }
    }

    public int getX() {
        return xvalue;
    }

    public int getY() {
        return yvalue;
    }
}

/**
 * A dialog of which exits are connected for terrain.
 */
class ExitsDialog extends JDialog implements ActionListener {
    private JCheckBox cheExit0 = new JCheckBox("0"); //$NON-NLS-1$
    private JCheckBox cheExit1 = new JCheckBox("1"); //$NON-NLS-1$
    private JCheckBox cheExit2 = new JCheckBox("2"); //$NON-NLS-1$
    private JCheckBox cheExit3 = new JCheckBox("3"); //$NON-NLS-1$
    private JCheckBox cheExit4 = new JCheckBox("4"); //$NON-NLS-1$
    private JCheckBox cheExit5 = new JCheckBox("5"); //$NON-NLS-1$
    private JLabel labBlank = new JLabel(""); //$NON-NLS-1$
    private JPanel panNorth = new JPanel(new GridBagLayout());
    private JPanel panSouth = new JPanel(new GridBagLayout());
    private JPanel panWest = new JPanel(new BorderLayout());
    private JPanel panEast = new JPanel(new BorderLayout());
    private JPanel panExits = new JPanel(new BorderLayout());
    private JButton butDone = new JButton(Messages.getString("BoardEditor.Done")); //$NON-NLS-1$

    ExitsDialog(JFrame frame) {
        super(frame, Messages.getString("BoardEditor.SetExits"), true); //$NON-NLS-1$
        setResizable(false);
        butDone.addActionListener(this);
        panNorth.add(cheExit0);
        panSouth.add(cheExit3);
        panWest.add(cheExit5, BorderLayout.NORTH);
        panWest.add(cheExit4, BorderLayout.SOUTH);
        panEast.add(cheExit1, BorderLayout.NORTH);
        panEast.add(cheExit2, BorderLayout.SOUTH);
        panExits.add(panNorth, BorderLayout.NORTH);
        panExits.add(panWest, BorderLayout.WEST);
        panExits.add(labBlank, BorderLayout.CENTER);
        panExits.add(panEast, BorderLayout.EAST);
        panExits.add(panSouth, BorderLayout.SOUTH);
        setLayout(new BorderLayout());
        add(panExits, BorderLayout.CENTER);
        add(butDone, BorderLayout.SOUTH);
        pack();
        setLocation(frame.getLocation().x + frame.getSize().width / 2 - getSize().width / 2,
                frame.getLocation().y + frame.getSize().height / 2 - getSize().height / 2);
    }

    public void setExits(int exits) {
        cheExit0.setSelected((exits & 1) != 0);
        cheExit1.setSelected((exits & 2) != 0);
        cheExit2.setSelected((exits & 4) != 0);
        cheExit3.setSelected((exits & 8) != 0);
        cheExit4.setSelected((exits & 16) != 0);
        cheExit5.setSelected((exits & 32) != 0);
    }

    public int getExits() {
        int exits = 0;
        exits |= cheExit0.isSelected() ? 1 : 0;
        exits |= cheExit1.isSelected() ? 2 : 0;
        exits |= cheExit2.isSelected() ? 4 : 0;
        exits |= cheExit3.isSelected() ? 8 : 0;
        exits |= cheExit4.isSelected() ? 16 : 0;
        exits |= cheExit5.isSelected() ? 32 : 0;
        return exits;
    }

    public void actionPerformed(ActionEvent actionEvent) {
        setVisible(false);
    }
}
