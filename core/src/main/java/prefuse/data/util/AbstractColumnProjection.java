package prefuse.data.util;

import prefuse.data.event.ProjectionListener;
import prefuse.util.collections.CopyOnWriteArrayList;

/**
 * Abstract base class for column projection instances. Implements the
 * listener functionality.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public abstract class AbstractColumnProjection implements ColumnProjection {

    // ------------------------------------------------------------------------
    // Listener Methods
    
    private CopyOnWriteArrayList m_listeners;
    
    /**
     * @see prefuse.data.util.ColumnProjection#addProjectionListener(prefuse.data.event.ProjectionListener)
     */
    public void addProjectionListener(ProjectionListener listener) {
        if ( m_listeners == null )
            m_listeners = new CopyOnWriteArrayList();
        if ( !m_listeners.contains(listener) )
            m_listeners.add(listener);
    }

    /**
     * @see prefuse.data.util.ColumnProjection#removeProjectionListener(prefuse.data.event.ProjectionListener)
     */
    public void removeProjectionListener(ProjectionListener listener) {
        if ( m_listeners != null ) {
            m_listeners.remove(listener);
            if (m_listeners.size() == 0)
                m_listeners = null;
        }
    }
    
    public void fireUpdate() {
        if ( m_listeners == null )
            return;
        Object[] listeners = m_listeners.getArray();
        for ( int i=0; i<listeners.length; ++i ) {
            ((ProjectionListener)listeners[i]).projectionChanged(this);
        }
    }
    
} // end of abstract class AbstractColumnProjection
