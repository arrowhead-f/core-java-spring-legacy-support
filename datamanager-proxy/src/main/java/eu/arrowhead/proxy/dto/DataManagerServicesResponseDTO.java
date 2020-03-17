package eu.arrowhead.proxy.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DataManagerServicesResponseDTO implements Serializable {

        //=================================================================================================
        // members
        
        private static final long serialVersionUID = 2184859722224129210L;
        
        private List<String> services = new ArrayList<>();
                
        //=================================================================================================
        // methods
        
        //-------------------------------------------------------------------------------------------------
        public DataManagerServicesResponseDTO() {}
        
        //-------------------------------------------------------------------------------------------------
        public List<String> getServices() { return services; }

        //-------------------------------------------------------------------------------------------------
        public void setServices(final List<String> services) { this.services = services; }
	
}