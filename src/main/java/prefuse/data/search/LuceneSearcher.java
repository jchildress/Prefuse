package prefuse.data.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import java.io.IOException;
import java.util.HashMap;

/**
 * Adapter class for interfacing with the Lucene search engine. By default,
 * instances of this class use an in-memory search index for English language
 * text, for use within a single application session. The class can, however,
 * be parametrized for any number of other configurations, including accessing
 * persistent search indices.
 *  
 * @version 1.0
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class LuceneSearcher {

    /**
     * Default Document field used to index text.
     */
    public static final String FIELD = "prefux-text";
    /**
     * Document field used to store the document ID number.
     */
    public static final String ID = "prefux-id";

    private Directory directory;
    private Analyzer analyzer;
    private String[] fields;

    private IndexSearcher searcher;
    private IndexReader reader;
    private IndexWriter writer;
    private boolean m_readMode = true;
    private boolean m_readOnly = false;

    private HashMap<String, Integer> m_hitCountCache;

    /**
     * Create a new LuceneSearcher using an in-memory search index.
     */
    public LuceneSearcher() {
        this(new RAMDirectory(), FIELD);
    }

    /**
     * Create a new LuceneSearcher using the specified search index location.
     *
     * @param dir the Lucene Directory indicating the search index to use.
     */
    public LuceneSearcher(Directory dir) {
        this(dir, FIELD);
    }

    /**
     * Create a new LuceneSearcher using a specified search index location,
     * a particular Document field to index, and given read/write status.
     *
     * @param dir   the Lucene Directory indicating the search index to use.
     * @param field the Lucene Document field that should be indexed.
     */
    public LuceneSearcher(Directory dir, String field) {
        this(dir, new String[]{field});
    }

    /**
     * Create a new LuceneSearcher using a specified search index location,
     * a particular Document fields to index, and given read/write status.
     *
     * @param dir    the Lucene Directory indicating the search index to use.
     * @param fields the Lucene Document fields that should be indexed.
     */
    public LuceneSearcher(Directory dir, String[] fields) {
        m_hitCountCache = new HashMap<>();
        directory = dir;
        analyzer = new StandardAnalyzer();
        this.fields = (String[]) fields.clone();
        try {
            IndexWriterConfig cfg = new IndexWriterConfig(analyzer);
            writer = new IndexWriter(directory, cfg);
            writer.close();
            writer = null;
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    // ------------------------------------------------------------------------

    /**
     * Sets if this LuceneSearcher is in read mode or write mode. In read more
     * searches can be issued, in write mode new Documents can be indexed.
     * Read-only LuceneSearcher instances can not be put into write mode.
     *
     * @param mode true for read mode, false for write mode.
     * @return true if the mode was successfully set, false otherwise.
     */
    public boolean setReadMode(boolean mode) {
        // return false if this is read-only
        if (m_readOnly && mode == false) return false;
        // do nothing if already in the mode
        if (m_readMode == mode) return true;
        // otherwise switch modes
        if (!mode) {
            // close any open searcher and reader
            try {
                // if ( searcher != null ) searcher.close();
                if (reader != null) reader.close();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            // open the writer
            try {
                IndexWriterConfig cfg = new IndexWriterConfig(analyzer);
                writer = new IndexWriter(directory, cfg);
            } catch (IOException e1) {
                e1.printStackTrace();
                return false;
            }
        } else {
            // optimize index and close writer
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
                return false;
            }
            // open the reader and searcher
            try {
                reader = DirectoryReader.open(directory);
                searcher = new IndexSearcher(reader);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        m_readMode = mode;
        return true;
    }

    /**
     * Searches the Lucene index using the given query String, returns an object
     * which provides access to the search results.
     *
     * @param query the search query
     * @return the search Hits
     * @throws ParseException        if the query is not parsed successfully
     * @throws IOException           if an input/ouput error occurs
     * @throws IllegalStateException if the searcher is in write mode
     */
    public TopDocs search(String query) throws IOException, ParseException {
        if (m_readMode) {
            Query q;
            if (fields.length == 1) {
                QueryParser parser = new QueryParser(fields[0], analyzer);
                q = parser.parse(query);
            } else {
                MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer);
                q = parser.parse(query);
            }
            return searcher.search(q, 100);
        } else {
            throw new IllegalStateException(
                    "Searches can only be performed when " +
                            "the LuceneSearcher is in read mode");
        }
    }

    /**
     * Return the result count for the given search query. To allow quick
     * repeated look ups, the hit count is cached (this cache is cleared
     * whenever a change to the search index occurs).
     *
     * @param query the search query
     * @return the number of matches to the query
     * @throws ParseException        if the query is not parsed successfully
     * @throws IOException           if an input/ouput error occurs
     * @throws IllegalStateException if the searcher is in write mode
     */
    public int numHits(String query) throws ParseException, IOException {
        Integer count;
        if ((count = m_hitCountCache.get(query)) == null) {
            TopDocs hits = search(query);
            count = new Integer(hits.totalHits);
            m_hitCountCache.put(query, count);
        }
        return count.intValue();
    }

    /**
     * Add a document to the Lucene search index.
     *
     * @param d the Document to add
     * @throws IllegalStateException if the searcher is not in write mode
     */
    public void addDocument(Document d) {
        if (!m_readMode) {
            try {
                writer.addDocument(d);
                m_hitCountCache.clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            throw new IllegalStateException(
                    "Documents can not be added to the index unless" +
                            "the LuceneSearcher is not in read mode");
        }
    }

    /**
     * Returns the Analyzer used to process text. See Lucene documentation
     * for more details.
     *
     * @return returns the analyzer.
     */
    public Analyzer getAnalyzer() {
        return analyzer;
    }

    /**
     * Sets the Analyzer used to process text. See Lucene documentation
     * for more details.
     *
     * @param analyzer the analyzer to set
     */
    public void setAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    /**
     * Returns the indexed Document fields. These fields determine which
     * fields are indexed as Documents are added and which fields are
     * queried over when searches are issued.
     *
     * @return returns the indexed Document fields
     */
    public String[] getFields() {
        return (String[]) fields.clone();
    }

    /**
     * Sets the indexed Document fields. These fields determine which
     * fields are indexed as Documents are added and which fields are
     * queried over when searches are issued.
     * param fields the indexed Document fields to use
     */
    public void setFields(String[] fields) {
        this.fields = (String[]) fields.clone();
    }

    /**
     * Returns the Lucene IndexReader. See Lucene documentation
     * for more details.
     *
     * @return teturns the IndexReader.
     */
    public IndexReader getIndexReader() {
        return reader;
    }

    /**
     * Returns the Lucene IndexSearcher. See Lucene documentation
     * for more details.
     *
     * @return returns the IndexSearcher.
     */
    public IndexSearcher getIndexSearcher() {
        return searcher;
    }

    /**
     * Indicates if ths LuceneSearcher is read-only.
     *
     * @return true if read-only, false if writes are allowed
     */
    public boolean isReadOnly() {
        return m_readOnly;
    }

} // end of class LuceneSearcher
