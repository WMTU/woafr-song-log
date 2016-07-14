package edu.mtu.wmtu;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.log4j.Logger;

import com.google.ras.api.core.AsyncCallback;
import com.google.ras.api.core.ErrorInfo;
import com.google.ras.api.core.Platform;
import com.google.ras.api.core.plugin.BasicWidget;
import com.google.ras.api.core.services.media.LibraryError;
import com.google.ras.api.core.services.media.LibraryService;
import com.google.ras.api.core.services.media.MediaAsset;
import com.google.ras.api.core.services.media.MediaAssetInfo;
import com.google.ras.api.core.services.playlist.EntryStatus;
import com.google.ras.api.core.services.playlist.EntryTypes;
import com.google.ras.api.core.services.playlist.PlaylistEntry;
import com.google.ras.api.core.services.scheduler.SchedulerAdapter;
import com.google.ras.api.core.services.scheduler.SchedulerService;
import com.google.ras.api.core.services.selection.Selection;
import com.google.ras.api.core.services.selection.SelectionAdapter;
import com.google.ras.api.core.services.selection.SelectionService;
import com.google.ras.api.core.ui.plaf.NButtonUI;
import com.google.ras.api.core.ui.plaf.styles.ButtonStyle;
import com.google.ras.api.core.ui.resources.PackageResources;
import com.google.ras.api.core.ui.resources.Resources;

public class LoggingWidget extends BasicWidget implements ActionListener {
  private static final Resources R = new PackageResources( LoggingWidget.class );
  private static final Logger log = Logger.getLogger( LoggingWidget.class );

  private SelectionService selectionService;
  private PlaylistEntry selectedEntry;
  private MediaAssetInfo selectedAsset;

  private SchedulerService schedulerService;

  private LibraryService libraryService;

  private static final Insets WEST_INSETS = new Insets( 5, 10, 5, 10 );
  private static final Insets EAST_INSETS = new Insets( 5, 10, 5, 10 );

  private String log_endpoint;
  private String log_username;
  private String log_password;

  private JLabel messageLabel;
  private JButton dropMeta;
  private JToggleButton toggleAutomation;
  private JLabel locationLabel;
  private JComboBox<String> location;
  private JLabel songTitleLabel;
  private JTextField songTitle;
  private JLabel songArtistLabel;
  private JTextField songArtist;
  private JLabel songAlbumLabel;
  private JTextField songAlbum;
  private JLabel songGenreLabel;
  private JComboBox<String> songGenre;
  private JButton addLog;
  private JButton clearForm;

  private String errorMessage;

  public LoggingWidget() throws FileNotFoundException, IOException {
    super( "Logging", R.getImage( "logging.png" ) );

    // Load config file
    InputStream input = getClass().getClassLoader().getResourceAsStream( "config.properties" );
    if ( input == null )
      throw new FileNotFoundException( "Widget configuration file not found in the classpath" );

    // Load properties into new config object
    Properties config = new Properties();
    config.load( input );
    log_endpoint = config.getProperty( "log_endpoint" );
    log_username = config.getProperty( "log_username" );
    log_password = config.getProperty( "log_password" );
    input.close();

    // Get the SelectionService and add listeners for selecting playlist entries
    // and media assets
    selectionService = Platform.getService( SelectionService.class );
    selectionService.addSelectionListener( new SelectionAdapter() {
      @Override
      public void entrySelectionChanged( Selection<PlaylistEntry> selection ) {
        setSelectedEntry( selection );
      }

      @Override
      public void mediaAssetSelectionChanged( Selection<MediaAssetInfo> selection ) {
        setSelectedAsset( selection );
      }
    } );

    // Get the SchedulerService and add a listener for the 'now playing' entry
    // on the Stack widget changing
    schedulerService = Platform.getService( SchedulerService.class );
    schedulerService.addListener( new SchedulerAdapter() {
      @Override
      public void currentEntryChanged() {
        checkChangedEntry();
      }
    } );

    // Get the LibraryService for uhh, future use
    libraryService = Platform.getService( LibraryService.class );
  }

  /**
   * Set up the layout and style of the widget, and add event listeners for
   * clicking on the buttons and pressing the enter key
   */
  @Override
  protected JComponent buildContentPanel() {
    getToolbar().setTitle( "WMTU DJ Logs" );

    // Set up container JPanel
    JPanel panel = new JPanel();
    panel.setBackground( Color.BLACK );
    panel.setLayout( new GridBagLayout() );

    // Set up ActionListener for enter key press
    KeyListener action = new KeyListener() {
      @Override
      public void keyPressed( KeyEvent k ) {
        if ( k.getKeyCode() == KeyEvent.VK_ENTER ) {
          parseLog();
        }
      }

      @Override
      public void keyTyped( KeyEvent e ) {
      }

      @Override
      public void keyReleased( KeyEvent e ) {
      }
    };

    // Initialize object variables
    GridBagConstraints gbc;
    Font newLabelFont;

    // Add message label
    gbc = createGbc( 0, 0 );
    gbc.gridwidth = 3;
    gbc.anchor = GridBagConstraints.CENTER;
    gbc.insets = new Insets( 10, 10, 10, 10 );
    gbc.weightx = 1.0;
    messageLabel = new JLabel( " ", JLabel.CENTER );
    messageLabel.setForeground( Color.WHITE );
    panel.add( messageLabel, gbc );

    // Add 'Fill Info' and toggle automation buttons
    gbc = createGbc( 0, 1 );
    gbc.anchor = GridBagConstraints.EAST;
    gbc.fill = GridBagConstraints.NONE;
    dropMeta = new JButton( "Fill Info" );
    dropMeta.setActionCommand( "meta" );
    dropMeta.addActionListener( this );
    dropMeta.setEnabled( false );
    panel.add( dropMeta, gbc );
    gbc = createGbc( 1, 1 );
    gbc.anchor = GridBagConstraints.EAST;
    gbc.fill = GridBagConstraints.NONE;
    toggleAutomation = new JToggleButton( "Log from Automation" );
    toggleAutomation.setUI( new NButtonUI( toggleAutomation, ButtonStyle.RED_GLOSSY ) );
    toggleAutomation.setActionCommand( "toggle" );
    toggleAutomation.addActionListener( this );
    panel.add( toggleAutomation, gbc );

    // Add bin location label and field
    gbc = createGbc( 0, 2 );
    locationLabel = new JLabel( "Location", JLabel.RIGHT );
    locationLabel.setForeground( new Color( 255, 255, 255 ) );
    newLabelFont = new Font( locationLabel.getFont().getName(), Font.BOLD, locationLabel.getFont().getSize() );
    locationLabel.setFont( newLabelFont );
    panel.add( locationLabel, gbc );
    gbc = createGbc( 1, 2 );
    String[] locations = { "Main Bin", "New Bin", "Personal", "Vinyl" };
    location = new JComboBox<String>( locations );
    location.setSelectedIndex( 2 );
    location.addKeyListener( action );
    panel.add( location, gbc );

    // Add song title label and field
    gbc = createGbc( 0, 3 );
    songTitleLabel = new JLabel( "Song Name", JLabel.RIGHT );
    songTitleLabel.setForeground( new Color( 255, 255, 255 ) );
    newLabelFont = new Font( songTitleLabel.getFont().getName(), Font.BOLD, songTitleLabel.getFont().getSize() );
    songTitleLabel.setFont( newLabelFont );
    panel.add( songTitleLabel, gbc );
    gbc = createGbc( 1, 3 );
    songTitle = new JTextField( 27 );
    songTitle.addKeyListener( action );
    panel.add( songTitle, gbc );

    // Add artist label and field
    gbc = createGbc( 0, 4 );
    songArtistLabel = new JLabel( "Artist or Group", JLabel.RIGHT );
    songArtistLabel.setForeground( new Color( 255, 255, 255 ) );
    newLabelFont = new Font( songArtistLabel.getFont().getName(), Font.BOLD, songArtistLabel.getFont().getSize() );
    songArtistLabel.setFont( newLabelFont );
    panel.add( songArtistLabel, gbc );
    gbc = createGbc( 1, 4 );
    songArtist = new JTextField( 27 );
    songArtist.addKeyListener( action );
    panel.add( songArtist, gbc );

    // Add album label and field
    gbc = createGbc( 0, 5 );
    songAlbumLabel = new JLabel( "Album", JLabel.RIGHT );
    songAlbumLabel.setForeground( new Color( 255, 255, 255 ) );
    newLabelFont = new Font( songAlbumLabel.getFont().getName(), Font.BOLD, songAlbumLabel.getFont().getSize() );
    songAlbumLabel.setFont( newLabelFont );
    panel.add( songAlbumLabel, gbc );
    gbc = createGbc( 1, 5 );
    songAlbum = new JTextField( 27 );
    songAlbum.addKeyListener( action );
    panel.add( songAlbum, gbc );

    // Add genre label and field
    gbc = createGbc( 0, 6 );
    songGenreLabel = new JLabel( "Genre", JLabel.RIGHT );
    songGenreLabel.setForeground( new Color( 255, 255, 255 ) );
    newLabelFont = new Font( songGenreLabel.getFont().getName(), Font.BOLD, songGenreLabel.getFont().getSize() );
    songGenreLabel.setFont( newLabelFont );
    panel.add( songGenreLabel, gbc );
    gbc = createGbc( 1, 6 );
    String[] genres = { " ", "Alternative", "Blues", "Classical", "Comedy", "Country", "Easy Listening", "Electronic",
        "Folk/Bluegrass", "Hip-Hop/Rap", "Holiday", "Jazz", "Local", "Metal", "Pop", "R&B/Funk/Soul", "Reggae", "Rock",
        "Soundtrack", "World" };
    songGenre = new JComboBox<String>( genres );
    songGenre.setSelectedIndex( 0 );
    songGenre.setEditable( true );
    songGenre.getEditor().getEditorComponent().addKeyListener( action );
    panel.add( songGenre, gbc );

    // Add buttons
    gbc = createGbc( 0, 7 );
    gbc.fill = GridBagConstraints.NONE;
    addLog = new JButton( "Add DJ Log" );
    addLog.setUI( new NButtonUI( addLog, ButtonStyle.GREEN_GLOSSY ) );
    addLog.setActionCommand( "submit" );
    addLog.addActionListener( this );
    panel.add( addLog, gbc );
    gbc = createGbc( 1, 7 );
    gbc.gridwidth = 1;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.NONE;
    clearForm = new JButton( "Clear Form" );
    clearForm.setActionCommand( "clear" );
    clearForm.addActionListener( this );
    panel.add( clearForm, gbc );

    return panel;
  }

  /**
   * Helper method for building GridBadConstraints given an object's position on
   * the grid
   * 
   * @param x
   *          x-coordinate of the object in the grid (increasing left to right)
   * @param y
   *          y-coordinate of the object in the grid (increasing top to bottom)
   * @return a GridBagConstraints object with default layout values
   */
  private GridBagConstraints createGbc( int x, int y ) {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = x;
    gbc.gridy = y;
    if ( x == 1 )
      gbc.gridwidth = 2;
    else
      gbc.gridwidth = 1;
    gbc.gridheight = 1;

    gbc.anchor = GridBagConstraints.EAST;
    gbc.fill = ( x == 0 ) ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;

    gbc.insets = ( x == 0 ) ? WEST_INSETS : EAST_INSETS;
    gbc.weightx = ( x == 0 ) ? 0.1 : 1.0;
    gbc.weighty = 1.0;
    return gbc;
  }

  /**
   * Attaches button press actions to methods
   */
  @Override
  public void actionPerformed( ActionEvent event ) {
    if ( event.getActionCommand().equals( "submit" ) ) {
      parseLog();
    } else if ( event.getActionCommand().equals( "clear" ) ) {
      messageLabel.setText( " " );
      clearLog();
    } else if ( event.getActionCommand().equals( "meta" ) ) {
      fillMeta();
    } else if ( event.getActionCommand().equals( "toggle" ) ) {
      toggleAutomation();
    }
  }

  /**
   * Validates and sanitizes log form input, then sends it to the Log app for
   * entry into the database and broadcast to services
   */
  private void parseLog() {
    if ( songTitle.getText().trim().equals( "" ) ) {
      messageLabel.setText( "Error: 'Song Name' cannot be blank" );
      messageLabel.setForeground( Color.ORANGE );
      return;
    }
    if ( songArtist.getText().trim().equals( "" ) ) {
      messageLabel.setText( "Error: 'Artist or Group' cannot be " + "blank" );
      messageLabel.setForeground( Color.ORANGE );
      return;
    }

    String regex = "(<([^>]+)>)";
    final String song = WordUtils.capitalizeFully( songTitle.getText().replaceAll( regex, "" ) );
    final String artist = WordUtils.capitalizeFully( songArtist.getText().replaceAll( regex, "" ) );
    final String album = WordUtils.capitalizeFully( songAlbum.getText().replaceAll( regex, "" ) );
    String genre = songGenre.getEditor().getItem().toString().replaceAll( regex, "" );

    boolean success = sendLog( song, artist, album, (String) location.getSelectedItem(), genre, "" );

    if ( success ) {
      Date rightNow = new Date();
      SimpleDateFormat dateFormat = new SimpleDateFormat( "h:mm a 'on' EEE',' MMM'.' d" );
      messageLabel.setText(
          "'" + song + "'" + " by " + artist + " was added to " + "the log at " + dateFormat.format( rightNow ) );
      messageLabel.setForeground( Color.GREEN );

      // Clear form fields
      clearLog();
    } else {
      messageLabel.setText( "Error: " + errorMessage );
      messageLabel.setForeground( Color.RED );
    }
  }

  /**
   * Clears the log form input fields and resets menus to default values
   */
  private void clearLog() {
    location.setSelectedIndex( 2 );
    songTitle.setText( "" );
    songArtist.setText( "" );
    songAlbum.setText( "" );
    songGenre.setSelectedIndex( 0 );
    songGenre.getEditor().setItem( "" );
  }

  /**
   * Fills the input form based on the fields in MediaAsset asset corresponding
   * to a playlist entry whose searh populates the results of this AsyncCallback
   */
  private AsyncCallback<MediaAsset, LibraryError> assetFromEntryFillMeta = new AsyncCallback<MediaAsset, LibraryError>() {
    public void onSuccess( MediaAsset asset ) {
      location.setSelectedIndex( 0 );
      songTitle.setText( asset.getTitle() );
      songArtist.setText( asset.getArtist() );
      songAlbum.setText( asset.getNote() );
      selectionService.clearSelectedEntry();
    }

    public void onFailure( ErrorInfo<LibraryError> error ) {
      // Display an error message to the user
      Platform.getMessageHandler().showError( error.message );
    }
  };

  /**
   * Fills the input form based on the fields in MediaAsset selectedAsset, or
   * initiates a search for a media asset matching the ID of PlaylistEntry
   * selectedEntry
   */
  private void fillMeta() {
    if ( selectedAsset != null ) {
      location.setSelectedIndex( 0 );
      songTitle.setText( selectedAsset.getTitle() );
      songArtist.setText( selectedAsset.getArtist() );
      songAlbum.setText( selectedAsset.getNote() );
      selectionService.clearSelectedMediaAsset();
    } else if ( selectedEntry != null ) {
      libraryService.getMediaAsset( selectedEntry.getAssetId(), assetFromEntryFillMeta );
    }
  }

  /**
   * Toggles input field enable/disable based on whether or not automatic
   * logging has been triggered
   */
  private void toggleAutomation() {
    if ( toggleAutomation.isSelected() ) {
      toggleAutomation.setText( "Log Manually" );
      location.setEnabled( false );
      songTitle.setEnabled( false );
      songArtist.setEnabled( false );
      songAlbum.setEnabled( false );
      songGenre.setEnabled( false );
      addLog.setEnabled( false );
      clearForm.setEnabled( false );
    } else {
      toggleAutomation.setText( "Log from Automation" );
      location.setEnabled( true );
      songTitle.setEnabled( true );
      songArtist.setEnabled( true );
      songAlbum.setEnabled( true );
      songGenre.setEnabled( true );
      addLog.setEnabled( true );
      clearForm.setEnabled( true );
    }
  }

  /**
   * Given song parameters, submit a record to the Log app
   * 
   * @param song
   *          Song title
   * @param artist
   *          Artist or Band name
   * @param album
   *          Album title
   * @param location
   *          Location of recording in media library
   * @param genre
   *          Genre
   * @param cdNumber
   *          ID of recording in media library
   * @return true on probable success, false on failure
   */
  private boolean sendLog( String song, String artist, String album, String location, String genre, String cdNumber ) {
    try {
      URL url = new URL( log_endpoint );

      // Build map of request parameters
      Map<String, Object> params = new LinkedHashMap<String, Object>();
      params.put( "location", location );
      params.put( "asset_id", cdNumber );
      params.put( "title", song );
      params.put( "artist", artist );
      params.put( "album", album );
      params.put( "genre", genre );

      // Form request body from parameter map
      StringBuilder postData = new StringBuilder();
      postData.append( '{' );
      for ( Map.Entry<String, Object> param : params.entrySet() ) {
        if ( postData.length() != 1 )
          postData.append( ',' );
        postData.append( '"' );
        postData.append( param.getKey() );
        postData.append( '"' );
        postData.append( ':' );
        postData.append( '"' );
        postData.append( String.valueOf( param.getValue() ) );
        postData.append( '"' );
      }
      postData.append( '}' );
      byte[] postDataBytes = postData.toString().getBytes( "UTF-8" );

      // Initiate POST request to Log app
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod( "POST" );
      conn.setRequestProperty( "Content-Type", "application/json" );
      conn.setRequestProperty( "Content-Length", String.valueOf( postDataBytes.length ) );
      conn.setDoOutput( true );
      conn.getOutputStream().write( postDataBytes );

      // Handle response from Log app
      int status = conn.getResponseCode();
      String message = conn.getResponseMessage();
      if ( status != 200 && status != 201 && status != 202 ) {
        errorMessage = String.format( "Log: HTTP %d: %s", status, message );
        return false;
      }
    } catch ( Exception e ) {
      errorMessage = ExceptionUtils.getStackTrace( e );
      e.printStackTrace();
      return false;
    }

    return true;
  }

  /**
   * Set or clear the selected PlaylistEntry
   * 
   * @param selection
   */
  private void setSelectedEntry( Selection<PlaylistEntry> selection ) {
    if ( selection != null ) {
      selectedEntry = selection.getFirstItem();
      dropMeta.setEnabled( true );
    } else {
      selectedEntry = null;
      dropMeta.setEnabled( false );
    }
  }

  /**
   * Set or clear the selected MediaAsset
   * 
   * @param selection
   */
  private void setSelectedAsset( Selection<MediaAssetInfo> selection ) {
    if ( selection != null ) {
      selectedAsset = selection.getFirstItem();
      dropMeta.setEnabled( true );
    } else {
      selectedAsset = null;
      dropMeta.setEnabled( false );
    }
  }

  /**
   * Submit a record to the Log app based on the fields in MediaAsset asset
   * corresponding to a playlist entry whose searh populates the results of this
   * AsyncCallback
   */
  private AsyncCallback<MediaAsset, LibraryError> assetFromEntryCheckChangedEntry = new AsyncCallback<MediaAsset, LibraryError>() {
    public void onSuccess( MediaAsset asset ) {
      // Enter log from automation
      final String song = asset.getTitle().trim();
      final String artist = asset.getArtist().trim();
      final String album = asset.getNote().trim();

      boolean success = sendLog( song, artist, album, "Main Bin", "", asset.getId().value() );

      if ( success ) {
        Date rightNow = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat( "h:mm a 'on' EEE',' MMM'.' d" );
        messageLabel.setText(
            "'" + song + "'" + " by " + artist + " was added to " + "the log at " + dateFormat.format( rightNow ) );
        messageLabel.setForeground( Color.GREEN );
      } else {
        messageLabel.setText( "Error: " + errorMessage );
        messageLabel.setForeground( Color.RED );
      }
    }

    public void onFailure( ErrorInfo<LibraryError> error ) {
      // Display an error message to the user
      Platform.getMessageHandler().showError( error.message );
    }
  };

  /**
   * When a new entry begins playing on the Stack widget, check a number of
   * criteria to determine if it is appropriate to automatically log the entry
   * to the Log app. If so, initiate a search for the media asset matching the
   * ID of PlaylistEntry selectedEntry
   */
  private void checkChangedEntry() {
    // Check if logging from automation is enabled
    if ( !toggleAutomation.isSelected() )
      return;

    // Get the current entry in the scheduler
    PlaylistEntry currentEntry = schedulerService.getCurrentEntry();

    // If the current entry is not playable digital audio, return
    if ( !EntryTypes.isPlayable( currentEntry ) )
      return;

    // If the current entry is not playing right now, return
    if ( currentEntry.getStatus() != EntryStatus.PLAYING )
      return;

    // If the current entry is a liner, underwriting, or station ID, return
    String entryAssetId = currentEntry.getAssetId().value().toUpperCase();
    if ( entryAssetId.contains( "LIN" ) || entryAssetId.contains( "COM" ) || entryAssetId.contains( "IDS" ) )
      return;

    // Get the media asset associated with the selected playlist entry and
    // finish processing the log
    libraryService.getMediaAsset( currentEntry.getAssetId(), assetFromEntryCheckChangedEntry );

    return;
  }

}
