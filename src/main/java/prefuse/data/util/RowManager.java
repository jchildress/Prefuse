package prefuse.data.util;

import prefuse.data.Table;
import prefuse.util.collections.IntIntSortedMap;
import prefuse.util.collections.IntIntTreeMap;
import prefuse.util.collections.IntIterator;


/**
 * Manages the set of valid rows for a Table instance, maintains an index of
 * the available and occupied rows. RowManager instances are used internally
 * by Table instances.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class RowManager {

    protected Table m_table;
    private IntIntSortedMap m_openRows;
    private int m_firstId = 0;
    private int m_curId = -1;
    
    // ------------------------------------------------------------------------
    // Constructor
    
    /**
     * Create a new RowManager for the given Table.
     * @param table the Table to manage
     */
    public RowManager(Table table) {
        m_table = table;
    }
    
    /**
     * Get the table managed by this RowManager.
     * @return the managed table
     */
    public Table getTable() {
        return m_table;
    }
    
    // ------------------------------------------------------------------------
    // Row Information Methods
    
    /**
     * Get the lowest-numbered occupied table row.
     * @return the minimum row
     */
    public int getMinimumRow() {
        return m_firstId;
    }

    /**
     * Get the highest-numbered occupied table row.
     * @return the maximum row
     */
    public int getMaximumRow() {
        return m_curId;
    }
    
    /**
     * Get the total number of occupied rows
     * @return the number of rows being used by the table
     */
    public int getRowCount() {
        return 1 + m_curId - m_firstId
            - (m_openRows ==null ? 0 : m_openRows.size());
    }
    
    /**
     * Indicates if a given row value is a valid, occupied row of the table.
     * @param row the row index to check
     * @return true if the row is valid and in use by the Table, false if
     * it is an illegal value or is currently free
     */
    public boolean isValidRow(int row) {
        return ( row >= m_firstId && row <= m_curId &&
                (m_openRows == null || !m_openRows.containsKey(row)) );
    }
    
    // ------------------------------------------------------------------------
    // Row Update Methods
    
    /**
     * Clear the row manager status, marking all rows as available.
     */
    public void clear() {
        m_openRows = null;
        m_firstId = 0;
        m_curId = -1;
    }
    
    /**
     * Add a new row to management. The lowest valued available row
     * will be used.
     * @return the row index of the newly added row
     */
    public int addRow() {
        int r;
        if ( m_openRows == null || m_openRows.isEmpty() ) {
            r = ( m_firstId == 0 ? ++m_curId : --m_firstId);
        } else {
            int key = m_openRows.firstKey();
            r = m_openRows.remove(key);
        }
        return r;
    }
    
    /**
     * Release a row and mark it as free.
     * @param row the row index of the released row
     * @return true if the row was successfully released, false if it
     * was already free or if the input is not a valid row index
     */
    public boolean releaseRow(int row) {
        if ( row < 0 ) {
            return false;
        } else if ( m_openRows != null && m_openRows.containsKey(row) ) {
            return false;
        } else if ( row == m_curId) {
            --m_curId;
        } else if ( row == m_firstId) {
            ++m_firstId;
        } else {
            if ( m_openRows == null )
                m_openRows = new IntIntTreeMap(false);
            m_openRows.put(row, row);
        }
        return true;
    }
    
    // ------------------------------------------------------------------------
    // Column Mapping
    
    /**
     * Given Table row and column indices, return the corresponding row in
     * the underlying data column. This is of use for CascadedTable instances,
     * which may reveal only a limited set of a parent table's rows and so
     * must map between table rows and the actual indices of the inherited
     * data columns.
     * @param row the table row
     * @param col the table column
     * @return the row value for accessing the correct value of the
     * referenced data column.
     */
    public int getColumnRow(int row, int col) {
        return this.isValidRow(row) ? row : -1;
    }
    
    /**
     * Given a column row index and a table column index, return the
     * table row corresponding to the column value. This is of use for
     * CascadedTable instances, which may reveal only a limited set of a parent
     * table's rows and so must map between table rows and the actual indices
     * of the inherited data columns.
     * @param columnRow the row of the underlying data column
     * @param col the table column
     * @return the row value for the Table that corresponds to the
     * given column row
     */
    public int getTableRow(int columnRow, int col) {
        return this.isValidRow(columnRow) ? columnRow : -1;
    }

    /**
     * Return an iterator over column row indices.
     * @param col the table column index
     * @return an iterator over column row indices corresponding
     * to valid rows of this RowManager
     */
    public IntIterator columnRows(int col) {
        return new ColumnRowIterator(rows(), col);
    }

    /**
     * Return an iterator over column row indices.
     * @param col the table column index
     * @param reverse indicates the direction to iterate over, true
     * for reverse, false for normal
     * @return an iterator over column row indices corresponding
     * to valid rows of this RowManager
     */
    public IntIterator columnRows(int col, boolean reverse) {
        return new ColumnRowIterator(rows(reverse), col);
    }
    
    /**
     * Return an iterator over column row indices.
     * @param rows an iterator over table row indices
     * @param col the table column index
     * @return an iterator over column row indices corresponding
     * to valid rows of this RowManager
     */
    public IntIterator columnRows(IntIterator rows, int col) {
        return new ColumnRowIterator(rows, col);
    }
    
    // ------------------------------------------------------------------------
    // Iterators
        
    /**
     * Get an iterator over the table rows.
     * @return an iterator over the table rows
     */
    public IntIterator rows() {
        return new RowIterator(false);
    }
    
    /**
     * Get an iterator over the table rows.
     * @param reverse indicates the direction to iterate over, true
     * for reverse, false for normal
     * @return an iterator over the table rows
     */
    public IntIterator rows(boolean reverse) {
        return new RowIterator(reverse);
    }
    
    /**
     * Iterator over the occupied rows of this RowManager.
     */
    public class RowIterator extends IntIterator {
        boolean reverse;
        int last = -1, next;

        public RowIterator(boolean reverse) {
            this.reverse = reverse;
            next = advance(reverse ? m_curId : m_firstId);
        }
        public boolean hasNext() {
            return ( reverse ? next >= 0 : next <= m_curId);
        }
        public int nextInt() {
            // advance the iterator
            last = next;
            next = advance(reverse ? --next : ++next);
            return last;
        }
        public void remove() {
            m_table.removeRow(last);
        }
        private final int advance(int idx) {
            if ( m_openRows == null )
                return idx;
            else if ( reverse )
                for (; idx >= 0 && m_openRows.containsKey(idx); --idx);
            else
                for (; idx <= m_curId && m_openRows.containsKey(idx); ++idx);
            return idx;
        }
    } // end of inner class RowIterator
    
    /**
     * Iterator over the indices into a given data column,
     * mapped to from the rows of this RowManager.
     */
    public class ColumnRowIterator extends IntIterator {
        private IntIterator rows;
        private int row;
        private int col;
        public ColumnRowIterator(IntIterator rows, int col) {
            this.rows = rows;
            this.col = col;
        }
        public boolean hasNext() {
            return rows.hasNext();
        }
        public int nextInt() {
            row = rows.nextInt();
            return getColumnRow(row, col);
        }
        public void remove() {
            m_table.removeRow(row);
        }
    } // end of inner class ColumnRowIterator
    
} // end of class RowManager
