2012-02-24
The psp-example-grouper-to-openldap example assumes that the 'etc:attribute:mailLocalAddress' attribute has been created in Grouper.

Either comment out the attribute from the GroupDataConnector in psp-resolver.xml :

  <resolver:DataConnector
    id="GroupDataConnector"
    xsi:type="grouper:GroupDataConnector">
    ...
    <!-- The "etc:attribute:mailLocalAddress" attribute framework definition. 
      <grouper:Attribute id="etc:attribute:mailLocalAddress" />
    -->
  </resolver:DataConnector>

or create the attribute via gsh

  gsh.sh mailLocalAddress.gsh
