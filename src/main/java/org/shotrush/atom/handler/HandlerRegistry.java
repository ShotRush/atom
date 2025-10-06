package org.shotrush.atom.handler;

import org.bukkit.event.Listener;
import org.shotrush.atom.Atom;
import org.shotrush.atom.api.AutoRegister;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class HandlerRegistry {
    private final List<SkillHandler> skillHandlers = new ArrayList<>();
    private final List<Listener> specialHandlers = new ArrayList<>();
    private final Atom plugin;
    
    public HandlerRegistry(Atom plugin) {
        this.plugin = plugin;
    }
    
    public void registerAll() {
        autoDiscoverHandlers();
        
        registerSkillHandler(new MiningHandler(plugin));
        registerSkillHandler(new CraftingHandler(plugin));
        registerSkillHandler(new SmeltingHandler(plugin));
        registerSkillHandler(new CombatHandler(plugin));
        registerSkillHandler(new FishingHandler(plugin));
        registerSkillHandler(new EnchantingHandler(plugin));
        registerSkillHandler(new BreedingHandler(plugin));
        registerSkillHandler(new RepairingHandler(plugin));
        registerSkillHandler(new BrewingHandler(plugin));
        registerSkillHandler(new FarmingHandler(plugin));
        
        registerSpecialHandler(new SpearHandler(plugin));
    }
    
    private void autoDiscoverHandlers() {
        try {
            String packageName = "org.shotrush.atom.handler";
            ClassLoader classLoader = plugin.getClass().getClassLoader();
            String path = packageName.replace('.', '/');
            Enumeration<URL> resources = classLoader.getResources(path);
            
            List<File> dirs = new ArrayList<>();
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                dirs.add(new File(resource.getFile()));
            }
            
            for (File directory : dirs) {
                findHandlers(directory, packageName);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Auto-discovery failed, using manual registration: " + e.getMessage());
        }
    }
    
    private void findHandlers(File directory, String packageName) {
        if (!directory.exists()) {
            return;
        }
        
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                findHandlers(file, packageName + "." + file.getName());
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                tryRegisterClass(className);
            }
        }
    }
    
    private void tryRegisterClass(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            
            if (!SkillHandler.class.isAssignableFrom(clazz)) {
                return;
            }
            
            if (clazz.isAnnotationPresent(AutoRegister.class)) {
                AutoRegister annotation = clazz.getAnnotation(AutoRegister.class);
                if (!annotation.value()) {
                    return;
                }
                
                Constructor<?> constructor = clazz.getConstructor(Atom.class);
                SkillHandler handler = (SkillHandler) constructor.newInstance(plugin);
                registerSkillHandler(handler);
                plugin.getLogger().info("Auto-registered handler: " + clazz.getSimpleName());
            }
        } catch (Exception e) {
        }
    }
    
    public void registerSkillHandler(SkillHandler handler) {
        if (!skillHandlers.contains(handler)) {
            skillHandlers.add(handler);
            handler.register();
        }
    }
    
    public void registerSpecialHandler(Listener handler) {
        if (!specialHandlers.contains(handler)) {
            specialHandlers.add(handler);
            org.bukkit.Bukkit.getPluginManager().registerEvents(handler, plugin);
        }
    }
    
    public void registerExternal(SkillHandler handler) {
        registerSkillHandler(handler);
        plugin.getLogger().info("Registered external handler: " + handler.getClass().getSimpleName());
    }
    
    public List<SkillHandler> getSkillHandlers() {
        return new ArrayList<>(skillHandlers);
    }
    
    public List<Listener> getSpecialHandlers() {
        return new ArrayList<>(specialHandlers);
    }
    
    public void unregisterAll() {
        skillHandlers.clear();
        specialHandlers.clear();
    }
}
