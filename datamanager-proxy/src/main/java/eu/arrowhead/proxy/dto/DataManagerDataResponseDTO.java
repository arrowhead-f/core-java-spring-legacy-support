package eu.arrowhead.proxy.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DataManagerDataResponseDTO implements Serializable {

    //=================================================================================================
    // members
        
	private static final long serialVersionUID = 8014029729703302404L;

	private List<SenML> data = new ArrayList<>();
            
    //=================================================================================================
    // methods
    
    //-------------------------------------------------------------------------------------------------
    public DataManagerDataResponseDTO() {}
    
    //-------------------------------------------------------------------------------------------------
    public List<SenML> getData() { return data; }

    //-------------------------------------------------------------------------------------------------
    public void setData(final List<SenML> data) { this.data = data; }
}