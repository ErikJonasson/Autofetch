/**
 * This file is part of Autofetch. Autofetch is free software: you can redistribute it and/or modify it under the terms of the Lesser GNU General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. Autofetch is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the Lesser GNU General Public License for more details. You should have received a copy of the Lesser GNU General Public
 * License along with Autofetch.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.autofetch.hibernate;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.hibernate.AnnotationException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.InvalidMappingException;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.common.reflection.MetadataProvider;
import org.hibernate.annotations.common.reflection.MetadataProviderInjector;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.cfg.AnnotationBinder;
import org.hibernate.cfg.AutofetchHbmBinder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.CopyIdentifierComponentSecondPass;
import org.hibernate.cfg.CreateKeySecondPass;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.ExtendsQueueEntry;
import org.hibernate.cfg.InheritanceState;
import org.hibernate.cfg.JPAIndexHolder;
import org.hibernate.cfg.Mappings;
import org.hibernate.cfg.MetadataSourceType;
import org.hibernate.cfg.PkDrivenByDefaultMapsIdSecondPass;
import org.hibernate.cfg.QuerySecondPass;
import org.hibernate.cfg.RecoverableException;
import org.hibernate.cfg.SecondPass;
import org.hibernate.cfg.SecondaryTableSecondPass;
import org.hibernate.cfg.SetSimpleValueTypeSecondPass;
import org.hibernate.cfg.SettingsFactory;
import org.hibernate.cfg.UniqueConstraintHolder;
import org.hibernate.cfg.annotations.reflection.JPAMetadataProvider;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ClassLoaderHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.JoinedIterator;
import org.hibernate.internal.util.xml.XmlDocument;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.service.ServiceRegistry;
import org.jboss.logging.Logger;

import javax.persistence.Entity;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Based on org.hibernate.cfg.Configuration.
 * <p>
 * Changes behavior of hibernate configuration to use our own HbmBinder and to
 * wrap session factories.
 *
 * @author Ali Ibrahim <aibrahim@cs.utexas.edu>
 */
public class AutofetchConfiguration extends Configuration {

    /**
     *
     */
    private List<CacheHolder> caches;
    private static final long serialVersionUID = 1L;
    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class,
            Configuration.class.getName());
    private boolean isDefaultProcessed = false;
    private ExtentManager extentManager;
    private EventListenerRegistry eventListenerRegistry;
    private SessionFactory sessionFactory;
    private ReflectionManager manager;
    private MetadataSourceQueue metadataSourceQueue = new MetadataSourceQueue();
    private boolean inSecondPass = false;

    public AutofetchConfiguration() {
        super();
        reset();
    }

    public AutofetchConfiguration(SettingsFactory arg0) {
        super(arg0);
        reset();
    }

    public SessionFactory buildSessionFactory(ServiceRegistry serviceRegistry, ExtentManager extentManager)
            throws HibernateException {
        this.extentManager = extentManager;
        setInterceptor(new AutofetchInterceptor(EmptyInterceptor.INSTANCE, extentManager));
        sessionFactory = super.buildSessionFactory(serviceRegistry);
        manager = getReflectionManager();
        return sessionFactory;
    }

    @Override
    public SessionFactory buildSessionFactory(ServiceRegistry serviceRegistry) throws HibernateException {
        sessionFactory = super.buildSessionFactory(serviceRegistry);
        manager = getReflectionManager();
        return sessionFactory;
    }

    private static boolean isOrmXml(XmlDocument xmlDocument) {
        return "entity-mappings".equals(xmlDocument.getDocumentTree().getRootElement().getName());
    }

    @Override
    public void add(XmlDocument metadataXml) {
        if (inSecondPass || !isOrmXml(metadataXml)) {
            metadataSourceQueue.add(metadataXml);
        } else {
            final MetadataProvider metadataProvider = ((MetadataProviderInjector) manager).getMetadataProvider();
            JPAMetadataProvider jpaMetadataProvider = (JPAMetadataProvider) metadataProvider;
            List<String> classNames = jpaMetadataProvider.getXMLContext().addDocument(metadataXml.getDocumentTree());
            for (String className : classNames) {
                try {
                    metadataSourceQueue.add(manager.classForName(className, this.getClass()));
                } catch (ClassNotFoundException e) {
                    throw new AnnotationException("Unable to load class defined in XML: " + className, e);
                }
            }
            jpaMetadataProvider.getXMLContext().applyDiscoveredAttributeConverters(this);
        }
    }

    private void originalSecondPassCompile() throws MappingException {
        LOG.debug("Processing extends queue");
        processExtendsQueue();

        LOG.debug("Processing collection mappings");
        Iterator itr = secondPasses.iterator();
        while (itr.hasNext()) {
            SecondPass sp = (SecondPass) itr.next();
            if (!(sp instanceof QuerySecondPass)) {
                sp.doSecondPass(classes);
                itr.remove();
            }
        }

        LOG.debug("Processing native query and ResultSetMapping mappings");
        itr = secondPasses.iterator();
        while (itr.hasNext()) {
            SecondPass sp = (SecondPass) itr.next();
            sp.doSecondPass(classes);
            itr.remove();
        }

        LOG.debug("Processing association property references");

        itr = propertyReferences.iterator();
        while (itr.hasNext()) {
            Mappings.PropertyReference upr = (Mappings.PropertyReference) itr.next();

            PersistentClass clazz = getClassMapping(upr.referencedClass);
            if (clazz == null) {
                throw new MappingException("property-ref to unmapped class: " + upr.referencedClass);
            }

            Property prop = clazz.getReferencedProperty(upr.propertyName);
            if (upr.unique) {
                ((SimpleValue) prop.getValue()).setAlternateUniqueKey(true);
            }
        }

        // TODO: Somehow add the newly created foreign keys to the internal collection

        LOG.debug("Creating tables' unique integer identifiers");
        LOG.debug("Processing foreign key constraints");

        itr = getTableMappings();
        int uniqueInteger = 0;
        Set<ForeignKey> done = new HashSet<>();
        while (itr.hasNext()) {
            Table table = (Table) itr.next();
            table.setUniqueInteger(uniqueInteger++);
            secondPassCompileForeignKeys(table, done);
        }
    }

    public static <T> List<T> createListOfType(Class<T> type) {
        return new ArrayList<>();
    }

    @Override
    public void setCollectionCacheConcurrencyStrategy(String collectionRole, String concurrencyStrategy, String region) {
        caches.add(new CacheHolder(collectionRole, concurrencyStrategy, region, false, false));
    }

    @Override
    public void setCacheConcurrencyStrategy(
            String entityName,
            String concurrencyStrategy,
            String region,
            boolean cacheLazyProperty) throws MappingException {
        caches.add(new CacheHolder(entityName, concurrencyStrategy, region, true, cacheLazyProperty));
    }

    @Override
    protected void secondPassCompile() throws MappingException {
        LOG.trace("Starting secondPassCompile() processing");

        // TEMPORARY
        // Ensure the correct ClassLoader is used in commons-annotations.
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(ClassLoaderHelper.getContextClassLoader());

        // process default values first
        {
            if (!isDefaultProcessed) {
                // use global delimiters if orm.xml declare it
                manager = getReflectionManager();
                Map defaults = manager.getDefaults();
                final Object isDelimited = defaults.get("delimited-identifier");
                if (isDelimited != null && isDelimited == Boolean.TRUE) {
                    getProperties().put(Environment.GLOBALLY_QUOTED_IDENTIFIERS, "true");
                }
                // Set default schema name if orm.xml declares it.
                final String schema = (String) defaults.get("schema");
                if (StringHelper.isNotEmpty(schema)) {
                    getProperties().put(Environment.DEFAULT_SCHEMA, schema);
                }
                // Set default catalog name if orm.xml declares it.
                final String catalog = (String) defaults.get("catalog");
                if (StringHelper.isNotEmpty(catalog)) {
                    getProperties().put(Environment.DEFAULT_CATALOG, catalog);
                }

                AnnotationBinder.bindDefaults(createMappings());
                isDefaultProcessed = true;
            }
        }

        // process metadata queue
        {

            Class<?> clazz;
            try {
                clazz = Class.forName("org.hibernate.cfg.Configuration");
                metadataSourceQueue.syncAnnotatedClasses();
                Method determineMetadataSourcePrecedence = clazz.getDeclaredMethod("determineMetadataSourcePrecedence");
                determineMetadataSourcePrecedence.setAccessible(true);
                metadataSourceQueue
                        .processMetadata((List<MetadataSourceType>) determineMetadataSourcePrecedence.invoke(this));
                // metadataSourceQueue.processMetadata(determineMetadataSourcePrecedence());
            } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException
                    | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        try {
            Class<?> clazz = Class.forName("org.hibernate.cfg.Configuration");
            inSecondPass = true;
            Method m1 = clazz.getDeclaredMethod("processSecondPassesOfType", Class.class);
            m1.setAccessible(true);
            m1.invoke(this, PkDrivenByDefaultMapsIdSecondPass.class);
            m1.invoke(this, SetSimpleValueTypeSecondPass.class);
            m1.invoke(this, CopyIdentifierComponentSecondPass.class);

            Method m2 = clazz.getDeclaredMethod("processFkSecondPassInOrder");
            m2.setAccessible(true);
            // not sure if this is the correct way to call function without parameters
            m2.invoke(this);

            m1.invoke(this, CreateKeySecondPass.class);
            m1.invoke(this, SecondaryTableSecondPass.class);

            // processSecondPassesOfType(PkDrivenByDefaultMapsIdSecondPass.class);
            // processSecondPassesOfType(SetSimpleValueTypeSecondPass.class);
            // processSecondPassesOfType(CopyIdentifierComponentSecondPass.class);
            // processFkSecondPassInOrder();
            // processSecondPassesOfType(CreateKeySecondPass.class);
            // processSecondPassesOfType(SecondaryTableSecondPass.class);

            originalSecondPassCompile();

            inSecondPass = false;
        } catch (RecoverableException | ClassNotFoundException | NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            // the exception was not recoverable after all
            throw (RuntimeException) e.getCause();
        }

        // process cache queue
        {
            Field field;
            try {
                field = Configuration.class.getDeclaredField("caches");

                field.setAccessible(true);
                Object cachesRefl = field.get(this);
                caches = (List<CacheHolder>) cachesRefl;
                for (CacheHolder holder : caches) {
                    if (holder.isClass) {
                        applyCacheConcurrencyStrategy(holder);
                    } else {
                        applyCollectionCacheConcurrencyStrategy(holder);
                    }
                }
                caches.clear();
            } catch (NoSuchFieldException | SecurityException | IllegalAccessException | IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        Class<?> clazz;
        try {
            clazz = Class.forName("org.hibernate.cfg.Configuration");

            Method buildUniqueKeyFromColumnNames1 = clazz.getDeclaredMethod("buildUniqueKeyFromColumnNames",
                    Table.class, String.class, String[].class);
            Method buildUniqueKeyFromColumnNames2 = clazz.getDeclaredMethod("buildUniqueKeyFromColumnNames",
                    Table.class, String.class, String[].class, String[].class, boolean.class);
            buildUniqueKeyFromColumnNames1.setAccessible(true);
            buildUniqueKeyFromColumnNames2.setAccessible(true);
            Field field = Configuration.class.getDeclaredField("uniqueConstraintHoldersByTable");
            field.setAccessible(true);
            Object uniqueConstraintHoldersByTableRefl = field.get(this);
            Map<Table, List<UniqueConstraintHolder>> uniqueConstraintHoldersByTable = (Map<Table, List<UniqueConstraintHolder>>) uniqueConstraintHoldersByTableRefl;

            for (Map.Entry<Table, List<UniqueConstraintHolder>> tableListEntry : uniqueConstraintHoldersByTable
                    .entrySet()) {
                final Table table = tableListEntry.getKey();
                final List<UniqueConstraintHolder> uniqueConstraints = tableListEntry.getValue();
                for (UniqueConstraintHolder holder : uniqueConstraints) {
                    buildUniqueKeyFromColumnNames1.invoke(this, table, holder.getName(), holder.getColumns());
                }
            }
            Field field1 = Configuration.class.getDeclaredField("jpaIndexHoldersByTable");
            field1.setAccessible(true);
            Object jpaIndexHoldersByTable1 = field1.get(this);
            Map<Table, List<JPAIndexHolder>> jpaIndexHoldersByTable = (Map<Table, List<JPAIndexHolder>>) jpaIndexHoldersByTable1;

            for (Table table : jpaIndexHoldersByTable.keySet()) {
                final List<JPAIndexHolder> jpaIndexHolders = jpaIndexHoldersByTable.get(table);
                for (JPAIndexHolder holder : jpaIndexHolders) {
                    buildUniqueKeyFromColumnNames2.invoke(this, table, holder.getName(), holder.getColumns(),
                            holder.getOrdering(), holder.isUnique());
                }
            }
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | NoSuchFieldException
                | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Thread.currentThread().setContextClassLoader(tccl);
    }

    private int processExtendsQueue() {
        LOG.debug("Processing extends queue");
        int added = 0;
        ExtendsQueueEntry extendsQueueEntry = findPossibleExtends();
        while (extendsQueueEntry != null) {
            metadataSourceQueue.processHbmXml(extendsQueueEntry.getMetadataXml(), extendsQueueEntry.getEntityNames());
            extendsQueueEntry = findPossibleExtends();
        }

        if (extendsQueue.size() > 0) {
            Iterator iterator = extendsQueue.keySet().iterator();
            StringBuilder buf = new StringBuilder("Following super classes referenced in extends not found: ");
            while (iterator.hasNext()) {
                final ExtendsQueueEntry entry = (ExtendsQueueEntry) iterator.next();
                buf.append(entry.getExplicitName());
                if (entry.getMappingPackage() != null) {
                    buf.append("[").append(entry.getMappingPackage()).append("]");
                }
                if (iterator.hasNext()) {
                    buf.append(",");
                }
            }
            throw new MappingException(buf.toString());
        }

        return added;
    }

    @Override
    protected ExtendsQueueEntry findPossibleExtends() {
        Iterator<ExtendsQueueEntry> itr = extendsQueue.keySet().iterator();
        while (itr.hasNext()) {
            final ExtendsQueueEntry entry = itr.next();
            boolean found = getClassMapping(entry.getExplicitName()) != null || getClassMapping(
                    AutofetchHbmBinder.getClassName(entry.getExplicitName(), entry.getMappingPackage())) != null;
            if (found) {
                itr.remove();
                return entry;
            }
        }
        return null;
    }

    /**
     * Ensures that any interceptor is wrapped with the AutofetchInterceptor.
     */
    @Override
    public Configuration setInterceptor(Interceptor i) {
        if (i instanceof AutofetchInterceptor) {
            return super.setInterceptor(i);
        } else {
            AutofetchInterceptor ai = (AutofetchInterceptor) getInterceptor();
            return super.setInterceptor(ai.copy(i));
        }
    }

    public ExtentManager getExtentManager() {
        return extentManager;
    }

    protected class MetadataSourceQueue implements Serializable {

        /**
         *
         */
        private static final long serialVersionUID = 1L;
        private LinkedHashMap<XmlDocument, Set<String>> hbmMetadataToEntityNamesMap = new LinkedHashMap<>();
        private Map<String, XmlDocument> hbmMetadataByEntityNameXRef = new HashMap<>();

        // XClass are not serializable by default
        private transient List<XClass> annotatedClasses = new ArrayList<>();
        // only used during the secondPhaseCompile pass, hence does not need to be
        // serialized
        private transient Map<String, XClass> annotatedClassesByEntityNameMap = new HashMap<>();

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ois.defaultReadObject();
            annotatedClassesByEntityNameMap = new HashMap<>();

            // build back annotatedClasses
            @SuppressWarnings("unchecked")
            List<Class> serializableAnnotatedClasses = (List<Class>) ois.readObject();
            annotatedClasses = new ArrayList<>(serializableAnnotatedClasses.size());
            for (Class clazz : serializableAnnotatedClasses) {
                annotatedClasses.add(manager.toXClass(clazz));
            }
        }

        private void writeObject(java.io.ObjectOutputStream out) throws IOException {
            out.defaultWriteObject();
            List<Class> serializableAnnotatedClasses = new ArrayList<>(annotatedClasses.size());
            for (XClass xClass : annotatedClasses) {
                serializableAnnotatedClasses.add(manager.toClass(xClass));
            }
            out.writeObject(serializableAnnotatedClasses);
        }

        public void add(XmlDocument metadataXml) {
            final Document document = metadataXml.getDocumentTree();
            final Element hmNode = document.getRootElement();
            Attribute packNode = hmNode.attribute("package");
            String defaultPackage = packNode != null ? packNode.getValue() : "";
            Set<String> entityNames = new HashSet<>();
            findClassNames(defaultPackage, hmNode, entityNames);
            for (String entity : entityNames) {
                hbmMetadataByEntityNameXRef.put(entity, metadataXml);
            }
            this.hbmMetadataToEntityNamesMap.put(metadataXml, entityNames);
        }

        private void findClassNames(String defaultPackage, Element startNode, Set<String> names) {
            // if we have some extends we need to check if those classes possibly could be
            // inside the
            // same hbm.xml file...
            Iterator[] classes = new Iterator[4];
            classes[0] = startNode.elementIterator("class");
            classes[1] = startNode.elementIterator("subclass");
            classes[2] = startNode.elementIterator("joined-subclass");
            classes[3] = startNode.elementIterator("union-subclass");

            Iterator classIterator = new JoinedIterator(classes);
            while (classIterator.hasNext()) {
                Element element = (Element) classIterator.next();
                String entityName = element.attributeValue("entity-name");
                if (entityName == null) {
                    entityName = getClassName(element.attribute("name"), defaultPackage);
                }
                names.add(entityName);
                findClassNames(defaultPackage, element, names);
            }
        }

        private String getClassName(Attribute name, String defaultPackage) {
            if (name == null) {
                return null;
            }
            String unqualifiedName = name.getValue();
            if (unqualifiedName == null) {
                return null;
            }
            if (unqualifiedName.indexOf('.') < 0 && defaultPackage != null) {
                return defaultPackage + '.' + unqualifiedName;
            }
            return unqualifiedName;
        }

        public void add(XClass annotatedClass) {
            annotatedClasses.add(annotatedClass);
        }

        protected void syncAnnotatedClasses() {
            final Iterator<XClass> itr = annotatedClasses.iterator();
            while (itr.hasNext()) {
                final XClass annotatedClass = itr.next();
                if (annotatedClass.isAnnotationPresent(Entity.class)) {
                    annotatedClassesByEntityNameMap.put(annotatedClass.getName(), annotatedClass);
                    continue;
                }

                if (!annotatedClass.isAnnotationPresent(javax.persistence.MappedSuperclass.class)) {
                    itr.remove();
                }
            }
        }

        protected void processMetadata(List<MetadataSourceType> order) {
            syncAnnotatedClasses();

            for (MetadataSourceType type : order) {
                if (MetadataSourceType.HBM.equals(type)) {
                    processHbmXmlQueue();
                } else if (MetadataSourceType.CLASS.equals(type)) {
                    processAnnotatedClassesQueue();
                }
            }
        }

        protected void processHbmXmlQueue() {
            LOG.debug("Processing hbm.xml files");
            for (Map.Entry<XmlDocument, Set<String>> entry : hbmMetadataToEntityNamesMap.entrySet()) {
                // Unfortunately we have to create a Mappings instance for each iteration here
                processHbmXml(entry.getKey(), entry.getValue());
            }
            hbmMetadataToEntityNamesMap.clear();
            hbmMetadataByEntityNameXRef.clear();
        }

        protected void processHbmXml(XmlDocument metadataXml, Set<String> entityNames) {
            try {
                AutofetchHbmBinder.bindRoot(metadataXml, createMappings(), Collections.EMPTY_MAP, entityNames);
            } catch (MappingException me) {
                throw new InvalidMappingException(metadataXml.getOrigin().getType(), metadataXml.getOrigin().getName(),
                        me);
            }

            for (String entityName : entityNames) {
                if (annotatedClassesByEntityNameMap.containsKey(entityName)) {
                    annotatedClasses.remove(annotatedClassesByEntityNameMap.get(entityName));
                    annotatedClassesByEntityNameMap.remove(entityName);
                }
            }
        }

        protected void processAnnotatedClassesQueue() {
            LOG.debug("Process annotated classes");
            // bind classes in the correct order calculating some inheritance state
            List<XClass> orderedClasses = orderAndFillHierarchy(annotatedClasses);
            Mappings mappings = createMappings();
            Map<XClass, InheritanceState> inheritanceStatePerClass = AnnotationBinder
                    .buildInheritanceStates(orderedClasses, mappings);

            for (XClass clazz : orderedClasses) {
                AnnotationBinder.bindClass(clazz, inheritanceStatePerClass, mappings);

                final String entityName = clazz.getName();
                if (hbmMetadataByEntityNameXRef.containsKey(entityName)) {
                    hbmMetadataToEntityNamesMap.remove(hbmMetadataByEntityNameXRef.get(entityName));
                    hbmMetadataByEntityNameXRef.remove(entityName);
                }
            }
            annotatedClasses.clear();
            annotatedClassesByEntityNameMap.clear();
        }

        protected List<XClass> orderAndFillHierarchy(List<XClass> original) {
            List<XClass> copy = new ArrayList<>(original);
            insertMappedSuperclasses(original, copy);

            // order the hierarchy
            List<XClass> workingCopy = new ArrayList<>(copy);
            List<XClass> newList = new ArrayList<>(copy.size());
            while (workingCopy.size() > 0) {
                XClass clazz = workingCopy.get(0);
                orderHierarchy(workingCopy, newList, copy, clazz);
            }
            return newList;
        }

        protected void insertMappedSuperclasses(List<XClass> original, List<XClass> copy) {
            for (XClass clazz : original) {
                XClass superClass = clazz.getSuperclass();
                while (superClass != null && !manager.equals(superClass, Object.class) && !copy.contains(superClass)) {
                    if (superClass.isAnnotationPresent(Entity.class)
                            || superClass.isAnnotationPresent(javax.persistence.MappedSuperclass.class)) {
                        copy.add(superClass);
                    }
                    superClass = superClass.getSuperclass();
                }
            }
        }

        private void orderHierarchy(List<XClass> copy, List<XClass> newList, List<XClass> original, XClass clazz) {
            if (clazz == null || manager.equals(clazz, Object.class)) {
                return;
            }
            // process superclass first
            orderHierarchy(copy, newList, original, clazz.getSuperclass());
            if (original.contains(clazz)) {
                if (!newList.contains(clazz)) {
                    newList.add(clazz);
                }
                copy.remove(clazz);
            }
        }

        public boolean isEmpty() {
            return hbmMetadataToEntityNamesMap.isEmpty() && annotatedClasses.isEmpty();
        }

    }

    private void applyCollectionCacheConcurrencyStrategy(CacheHolder holder) {
        Collection collection = getCollectionMapping(holder.role);
        if (collection == null) {
            throw new MappingException("Cannot cache an unknown collection: " + holder.role);
        }
        collection.setCacheConcurrencyStrategy(holder.usage);
        collection.setCacheRegionName(holder.region);
    }

    private void applyCacheConcurrencyStrategy(CacheHolder holder) {
        RootClass rootClass = getRootClassMapping(holder.role);
        if (rootClass == null) {
            throw new MappingException("Cannot cache an unknown entity: " + holder.role);
        }
        rootClass.setCacheConcurrencyStrategy(holder.usage);
        rootClass.setCacheRegionName(holder.region);
        rootClass.setLazyPropertiesCacheable(holder.cacheLazy);
    }

    RootClass getRootClassMapping(String clazz) throws MappingException {
        try {
            return (RootClass) getClassMapping(clazz);
        } catch (ClassCastException cce) {
            throw new MappingException("You may only specify a cache for root <class> mappings. Attempted on " + clazz);
        }
    }

    private static class CacheHolder {

        public CacheHolder(String role, String usage, String region, boolean isClass, boolean cacheLazy) {
            this.role = role;
            this.usage = usage;
            this.region = region;
            this.isClass = isClass;
            this.cacheLazy = cacheLazy;
        }

        public String role;
        public String usage;
        public String region;
        public boolean isClass;
        public boolean cacheLazy;
    }
}