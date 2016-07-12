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
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

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

  private String dbHostname;
  private String dbName;
  private String dbTable;
  private String dbUser;
  private String dbPassword;

  private String tuneInPartnerId;
  private String tuneInPartnerKey;
  private String tuneInStationId;

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

    InputStream input = getClass().getClassLoader().getResourceAsStream( "config.properties" );
    if ( input == null )
      throw new FileNotFoundException( "Widget configuration file not found in the classpath" );
    Properties config = new Properties();
    config.load( input );
    dbHostname = config.getProperty( "dbHostname" );
    dbName = config.getProperty( "dbName" );
    dbTable = config.getProperty( "dbTable" );
    dbUser = config.getProperty( "dbUser" );
    dbPassword = config.getProperty( "dbPassword" );
    tuneInPartnerId = config.getProperty( "tuneInPartnerId" );
    tuneInPartnerKey = config.getProperty( "tuneInPartnerKey" );
    tuneInStationId = config.getProperty( "tuneInStationId" );
    input.close();

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

    schedulerService = Platform.getService( SchedulerService.class );
    schedulerService.addListener( new SchedulerAdapter() {
      @Override
      public void currentEntryChanged() {
        checkChangedEntry();
      }
    } );

    libraryService = Platform.getService( LibraryService.class );
  }

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

    String truncArtist;
    if ( artist.length() > 4 && artist.substring( 0, 4 ).equals( "The " ) ) {
      truncArtist = artist.substring( 4 );
    } else {
      truncArtist = artist;
    }

    boolean success = insertLog( song, artist, album, (String) location.getSelectedItem(), genre, truncArtist, "" );

    new Timer().schedule( new TimerTask() {
      @Override
      public void run() {
        boolean success = tuneInLog( song, artist, album );
        if ( !success ) {
          messageLabel.setText( "Error: " + errorMessage );
          messageLabel.setForeground( Color.RED );
        }
      }
    }, 30000 );

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

  private void clearLog() {
    location.setSelectedIndex( 2 );
    songTitle.setText( "" );
    songArtist.setText( "" );
    songAlbum.setText( "" );
    songGenre.setSelectedIndex( 0 );
    songGenre.getEditor().setItem( "" );
  }

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

  private boolean insertLog( String song, String artist, String album, String location, String genre,
      String truncArtist, String cdNumber ) {
    String dbUrl = "jdbc:mysql://" + dbHostname + ":3306/" + dbName + "?useUnicode=true&characterEncoding=utf-8";
    String dbQuery = "INSERT INTO " + dbTable + " (ts, song_name, artist, album, location, genre, truncated_artist, "
        + "cd_number) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    try {
      Class.forName( "com.mysql.jdbc.Driver" ).newInstance();
      Connection conn = DriverManager.getConnection( dbUrl, dbUser, dbPassword );

      PreparedStatement statement = conn.prepareStatement( dbQuery );
      statement.setTimestamp( 1, new Timestamp( new Date().getTime() ) );
      statement.setString( 2, song );
      statement.setString( 3, artist );
      statement.setString( 4, album );
      statement.setString( 5, location );
      statement.setString( 6, genre );
      statement.setString( 7, truncArtist );
      statement.setString( 8, cdNumber );

      statement.execute();

      conn.close();
    } catch ( Exception e ) {
      errorMessage = ExceptionUtils.getStackTrace( e );
      e.printStackTrace();
      return false;
    }

    return true;
  }

  private boolean tuneInLog( String song, String artist, String album ) {
    String url = "http://air.radiotime.com/Playing.ashx";
    String charset = java.nio.charset.StandardCharsets.UTF_8.name();
    String query;

    try {
      query = String.format( "partnerId=%s&partnerKey=%s&id=%s&title=%s&artist=%s",
          URLEncoder.encode( tuneInPartnerId, charset ), URLEncoder.encode( tuneInPartnerKey, charset ),
          URLEncoder.encode( tuneInStationId, charset ), URLEncoder.encode( song, charset ),
          URLEncoder.encode( artist, charset ) );

      if ( !album.trim().isEmpty() )
        query += String.format( "&album=%s", URLEncoder.encode( album, charset ) );
    } catch ( UnsupportedEncodingException e ) {
      errorMessage = ExceptionUtils.getStackTrace( e );
      e.printStackTrace();
      return false;
    }

    try {
      HttpURLConnection connection = (HttpURLConnection) new URL( url + "?" + query ).openConnection();
      connection.setRequestProperty( "Accept-Charset", charset );
      connection.getInputStream();
      int status = connection.getResponseCode();
      String message = connection.getResponseMessage();
      if ( status != 200 ) {
        errorMessage = String.format( "TuneIn: HTTP %d: %s", status, message );
        return false;
      }
    } catch ( MalformedURLException e ) {
      errorMessage = ExceptionUtils.getStackTrace( e );
      e.printStackTrace();
      return false;
    } catch ( IOException e ) {
      errorMessage = ExceptionUtils.getStackTrace( e );
      e.printStackTrace();
      return false;
    }

    return true;
  }

  private void setSelectedEntry( Selection<PlaylistEntry> selection ) {
    if ( selection != null ) {
      selectedEntry = selection.getFirstItem();
      dropMeta.setEnabled( true );
    } else {
      selectedEntry = null;
      dropMeta.setEnabled( false );
    }
  }

  private void setSelectedAsset( Selection<MediaAssetInfo> selection ) {
    if ( selection != null ) {
      selectedAsset = selection.getFirstItem();
      dropMeta.setEnabled( true );
    } else {
      selectedAsset = null;
      dropMeta.setEnabled( false );
    }
  }

  private AsyncCallback<MediaAsset, LibraryError> assetFromEntryCheckChangedEntry = new AsyncCallback<MediaAsset, LibraryError>() {
    public void onSuccess( MediaAsset asset ) {
      // Enter log from automation
      final String song = WordUtils.capitalizeFully( asset.getTitle().trim() );
      final String artist = WordUtils.capitalizeFully( asset.getArtist().trim() );
      final String album = WordUtils.capitalizeFully( asset.getNote().trim() );
      String truncArtist;
      if ( artist.length() > 4 && artist.substring( 0, 4 ).equals( "The " ) ) {
        truncArtist = artist.substring( 4 );
      } else {
        truncArtist = artist;
      }

      boolean success = insertLog( song, artist, album, "Main Bin", "", truncArtist, asset.getId().value() );

      new Timer().schedule( new TimerTask() {
        @Override
        public void run() {
          boolean success = tuneInLog( song, artist, album );
          if ( !success ) {
            messageLabel.setText( "Error: " + errorMessage );
            messageLabel.setForeground( Color.RED );
          }
        }
      }, 30000 );

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
