GrouperSession.startRootSession();
Stem etcAttribute = StemFinder.findByName(GrouperSession.staticGrouperSession(), "etc:attribute", true);
AttributeDef attributeDef = etcAttribute.addChildAttributeDef("mailLocalAddressAttributeDef", AttributeDefType.attr);
attributeDef.setAssignToGroup(true);
attributeDef.setMultiValued(true);
attributeDef.setValueType(AttributeDefValueType.string);
attributeDef.store();
etcAttribute.addChildAttributeDefName(attributeDef, "mailLocalAddress", "mailLocalAddress");
