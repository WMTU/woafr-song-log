package edu.mtu.wmtu;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.log4j.Logger;

import com.google.ras.api.core.Platform;
import com.google.ras.api.core.plugin.BasicWidget;
import com.google.ras.api.core.services.media.MediaAssetInfo;
import com.google.ras.api.core.services.playlist.PlaylistEntry;
import com.google.ras.api.core.services.selection.Selection;
import com.google.ras.api.core.services.selection.SelectionAdapter;
import com.google.ras.api.core.services.selection.SelectionService;
import com.google.ras.api.core.ui.resources.PackageResources;
import com.google.ras.api.core.ui.resources.Resources;

public class LoggingWidget extends BasicWidget implements ActionListener {
  private static final Resources R =
      new PackageResources( LoggingWidget.class );
  private static final Logger log =
      Logger.getLogger( LoggingWidget.class );

  private SelectionService selectionService;
  private PlaylistEntry selectedEntry;
  private MediaAssetInfo selectedAsset;

  private static final Insets WEST_INSETS = new Insets( 5, 10, 5, 10 );
  private static final Insets EAST_INSETS = new Insets( 5, 10, 5, 10 );

  private String dbHostname;
  private String dbName;
  private String dbTable;
  private String dbUser;
  private String dbPassword;

  private JLabel messageLabel;
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
  private JButton dropMeta;

  private String errorMessage;

  public LoggingWidget() throws FileNotFoundException, IOException {
    super( "Logging", R.getImage("logging.png") );

    InputStream input = getClass().getClassLoader().getResourceAsStream(
      "config.properties" );
    if ( input == null )
      throw new FileNotFoundException(
        "Widget configuration file not found in the classpath" );
    Properties config = new Properties();
    config.load(input);
    dbHostname = config.getProperty( "dbHostname" );
    dbName = config.getProperty( "dbName" );
    dbTable = config.getProperty( "dbTable" );
    dbUser = config.getProperty( "dbUser" );
    dbPassword = config.getProperty( "dbPassword" );
    input.close();

    selectionService = Platform.getService( SelectionService.class );
    selectionService.addSelectionListener( new SelectionAdapter(){
      @Override
      public void entrySelectionChanged(Selection<PlaylistEntry> selection) {
        setSelectedEntry(selection);
      }

      @Override
      public void mediaAssetSelectionChanged(Selection<MediaAssetInfo> selection) {
        setSelectedAsset(selection);
      }
    });
  }

  @Override
  protected JComponent buildContentPanel() {
    getToolbar().setTitle( "WMTU DJ Logs" );

    // Set up container JPanel
    JPanel panel = new JPanel();
    panel.setBackground( Color.BLACK );
    panel.setLayout( new GridBagLayout() );

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

    // Add bin location label and field
    gbc = createGbc( 0, 1 );
    locationLabel = new JLabel( "Album Location", JLabel.RIGHT );
    locationLabel.setForeground( new Color( 227, 6, 48 ) );
    newLabelFont = new Font( locationLabel.getFont().getName(),
        Font.BOLD + Font.ITALIC, locationLabel.getFont().getSize() );
    locationLabel.setFont(newLabelFont);
    panel.add( locationLabel, gbc );
    gbc = createGbc( 1, 1 );
    String[] locations = { "Main Bin", "New Bin", "Personal", "Vinyl" };
    location = new JComboBox<String>( locations );
    location.setSelectedIndex( 2 );
    panel.add( location, gbc );

    // Add song title label and field
    gbc = createGbc( 0, 2 );
    songTitleLabel = new JLabel( "Song Name", JLabel.RIGHT );
    songTitleLabel.setForeground( new Color( 65, 227, 195 ) );
    newLabelFont = new Font( songTitleLabel.getFont().getName(),
        Font.BOLD + Font.ITALIC, songTitleLabel.getFont().getSize() );
    songTitleLabel.setFont(newLabelFont);
    panel.add( songTitleLabel, gbc );
    gbc = createGbc( 1, 2 );
    songTitle = new JTextField( 27 );
    panel.add( songTitle, gbc );

    // Add artist label and field
    gbc = createGbc( 0, 3 );
    songArtistLabel = new JLabel( "Artist or Group", JLabel.RIGHT );
    songArtistLabel.setForeground( new Color( 255, 243, 53 ) );
    newLabelFont = new Font( songArtistLabel.getFont().getName(),
        Font.BOLD + Font.ITALIC, songArtistLabel.getFont().getSize() );
    songArtistLabel.setFont(newLabelFont);
    panel.add( songArtistLabel, gbc );
    gbc = createGbc( 1, 3 );
    songArtist = new JTextField( 27 );
    panel.add( songArtist, gbc );

    // Add album label and field
    gbc = createGbc( 0, 4 );
    songAlbumLabel = new JLabel( "Album", JLabel.RIGHT );
    songAlbumLabel.setForeground( new Color( 235, 124, 43 ) );
    newLabelFont = new Font( songAlbumLabel.getFont().getName(),
        Font.BOLD + Font.ITALIC, songAlbumLabel.getFont().getSize() );
    songAlbumLabel.setFont(newLabelFont);
    panel.add( songAlbumLabel, gbc );
    gbc = createGbc( 1, 4 );
    songAlbum = new JTextField( 27 );
    panel.add( songAlbum, gbc );

    // Add genre label and field
    gbc = createGbc( 0, 5 );
    songGenreLabel = new JLabel( "Genre", JLabel.RIGHT );
    songGenreLabel.setForeground( new Color( 219, 45, 235 ) );
    newLabelFont = new Font( songGenreLabel.getFont().getName(),
        Font.BOLD + Font.ITALIC, songGenreLabel.getFont().getSize() );
    songGenreLabel.setFont(newLabelFont);
    panel.add( songGenreLabel, gbc );
    gbc = createGbc( 1, 5 );
    String[] genres = { " ", "Alternative", "Blues", "Classical", "Comedy",
        "Country", "Easy Listening", "Electronic", "Folk/Bluegrass",
        "Hip-Hop/Rap", "Holiday", "Jazz", "Local", "Metal", "Pop",
        "R&B/Funk/Soul", "Reggae", "Rock", "Soundtrack", "World" };
    songGenre = new JComboBox<String>( genres );
    songGenre.setSelectedIndex( 0 );
    songGenre.setEditable( true );
    panel.add( songGenre, gbc );

    // Add buttons
    gbc = createGbc( 0, 6 );
    gbc.fill = GridBagConstraints.NONE;
    JButton addLog = new JButton( "Add DJ Log" );
    addLog.setActionCommand( "submit" );
    addLog.addActionListener( this );
    panel.add( addLog, gbc );
    gbc = createGbc( 1, 6 );
    gbc.gridwidth = 1;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.NONE;
    JButton clearForm = new JButton( "Clear Form" );
    clearForm.setActionCommand( "clear" );
    clearForm.addActionListener( this );
    panel.add( clearForm, gbc );
    gbc = createGbc( 2, 6 );
    gbc.anchor = GridBagConstraints.EAST;
    gbc.fill = GridBagConstraints.NONE;
    dropMeta = new JButton( "Fill Info" );
    dropMeta.setActionCommand( "meta" );
    dropMeta.addActionListener( this );
    dropMeta.setEnabled( false );
    panel.add( dropMeta, gbc );

    return panel;
  }

  private GridBagConstraints createGbc( int x, int y ) {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = x;
    gbc.gridy = y;
    if ( x == 1 ) gbc.gridwidth = 2;
    else gbc.gridwidth = 1;
    gbc.gridheight = 1;

    gbc.anchor = GridBagConstraints.EAST;
    gbc.fill = (x == 0) ? GridBagConstraints.BOTH
        : GridBagConstraints.HORIZONTAL;

    gbc.insets = (x == 0) ? WEST_INSETS : EAST_INSETS;
    gbc.weightx = (x == 0) ? 0.1 : 1.0;
    gbc.weighty = 1.0;
    return gbc;
  }

  @Override
  public void actionPerformed(ActionEvent event) {
    if ( event.getActionCommand().equals( "submit" ) ) {
      parseLog();
    } else if ( event.getActionCommand().equals( "clear" ) ) {
      messageLabel.setText( " " );
      clearLog();
    } else if ( event.getActionCommand().equals( "meta" ) ) {
      fillMeta();
    }
  }

  private void parseLog() {
  if ( songTitle.getText().trim().equals( "" ) ) {
      messageLabel.setText( "Error: 'Song Name' cannot be blank" );
      messageLabel.setForeground( Color.ORANGE );
      return;
    }
    if ( songArtist.getText().trim().equals( "" ) ) {
      messageLabel.setText( "Error: 'Artist or Group' cannot be "
          + "blank" );
      messageLabel.setForeground( Color.ORANGE );
      return;
    }

    String regex = "(<([^>]+)>)";
    String song = songTitle.getText().replaceAll( regex, "" );
    String artist = songArtist.getText().replaceAll( regex, "" );
    String album = songAlbum.getText().replaceAll( regex, "" );
    String genre = (( String ) songGenre.getSelectedItem())
        .replaceAll( regex, "" );

    artist = WordUtils.capitalizeFully( artist );
    song = WordUtils.capitalizeFully( song );
    album = WordUtils.capitalizeFully( album );
    String truncArtist;
    if ( artist.length() > 4 && artist.substring( 0, 4 ).equals( "The " ) ) {
      truncArtist = artist.substring( 4 );
    } else {
      truncArtist = artist;
    }

    boolean success = insertLog( song, artist,
        album, ( String ) location.getSelectedItem(), genre,
        truncArtist );

    if ( success ) {
      Date rightNow = new Date();
      SimpleDateFormat dateFormat =
          new SimpleDateFormat( "h:mm a 'on' EEE',' MMM'.' d" );
      messageLabel.setText( "'" + song + "'"+ " by " + artist + " was added to "
          + "the log at " + dateFormat.format( rightNow ) );
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
  }

  private void fillMeta() {
    if ( selectedAsset != null ) {
      location.setSelectedIndex( 0 );
      songTitle.setText( selectedAsset.getTitle() );
      songArtist.setText( selectedAsset.getArtist() );
      songAlbum.setText( selectedAsset.getMetadataField( "Album" ) );
      if ( songAlbum.getText().isEmpty() )
        songAlbum.setText( selectedAsset.getNote() );
    } else if ( selectedEntry != null ) {
      location.setSelectedIndex( 0 );
      songTitle.setText( selectedEntry.getTitle() );
      songArtist.setText( selectedEntry.getArtist() );
      songAlbum.setText( selectedEntry.getField( "Album" ) );
      if ( songAlbum.getText().isEmpty() )
        songAlbum.setText( selectedEntry.getNote() );
    }
  }

  private boolean insertLog( String song, String artist, String album,
      String location, String genre, String truncArtist ) {
    String dbUrl = "jdbc:mysql://" + dbHostname
        + ":3306/" + dbName
        + "?useUnicode=true&characterEncoding=utf-8";
    String dbQuery = "INSERT INTO " + dbTable
        + " (ts, song_name, artist, album, location, genre, truncated_artist"
        + ") VALUES (?, ?, ?, ?, ?, ?, ?)";

    try {
      Class.forName( "com.mysql.jdbc.Driver" ).newInstance();
      Connection conn = DriverManager.getConnection( dbUrl, dbUser,
          dbPassword );

      PreparedStatement statement = conn.prepareStatement( dbQuery );
      statement.setTimestamp( 1, new Timestamp( new Date().getTime() ) );
      statement.setString( 2, song );
      statement.setString( 3, artist );
      statement.setString( 4, album );
      statement.setString( 5, location );
      statement.setString( 6, genre );
      statement.setString( 7, truncArtist );

      statement.execute();

      conn.close();

      return true;
    } catch ( Exception e ) {
      errorMessage = ExceptionUtils.getStackTrace( e );
      e.printStackTrace();
    }

    return false;
  }

  private void setSelectedEntry(Selection<PlaylistEntry> selection) {
    if (selection != null) {
      selectedEntry = selection.getFirstItem();
      dropMeta.setEnabled( true );
    } else {
      selectedEntry = null;
      dropMeta.setEnabled( false );
    }
  }

  private void setSelectedAsset(Selection<MediaAssetInfo> selection) {
    if (selection != null) {
      selectedAsset = selection.getFirstItem();
      dropMeta.setEnabled( true );
    } else {
      selectedAsset = null;
      dropMeta.setEnabled( false );
    }
  }

}
