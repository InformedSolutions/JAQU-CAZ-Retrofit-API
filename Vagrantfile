# -*- mode: ruby -*-
# vi: set ft=ruby :

VAGRANTFILE_API_VERSION = "2"

# Install vagrant reload plugin to support restarts during provisioning
unless Vagrant.has_plugin?("vagrant-reload")
  system("vagrant plugin install vagrant-reload")
  puts "Vagrant reload plugin installed, please try the command again."
  exit
end

unless Vagrant.has_plugin?("vagrant-disksize")
  system("vagrant plugin install vagrant-disksize")
  puts "Vagrant disksize plugin installed, please try the command again."
  exit
end

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|

  # Use ubuntu bionic base box with named version
  config.vm.box = "informed/ubuntu-bionic64-docker-desktop-eclipse-20190606"
  config.vm.box_version = "1.0.0"
  config.disksize.size = '20GB'
  config.ssh.insert_key = false
  
  # Update Virtualbox provider settings
  config.vm.provider "virtualbox" do |vb|
    vb.cpus = 3
    vb.memory = "4092"
    vb.name = "ubuntu-bionic64-docker-desktop-eclipse"

    # Enable virtualbox UI to attach on boot
    vb.gui = true

    # Increase default video RAM allocation 
    vb.customize ["modifyvm", :id, "--vram", "256"]

    # Enable 2D/3D graphics acceleration
    vb.customize ["modifyvm", :id, "--accelerate2dvideo", "on"]
    vb.customize ["modifyvm", :id, "--accelerate3d", "on"]

    # Enable bidirectional clipboard with drag and drop support
    vb.customize ["modifyvm", :id, "--clipboard", "bidirectional"]
    vb.customize ["modifyvm", :id, "--draganddrop", "bidirectional"]
	vb.customize ["modifyvm", :id, "--uartmode1", "disconnected"]
  end

  # Map host ports from range 8000 to 8050 for web applications from host to guest
  for i in 8000..8100
    config.vm.network :forwarded_port, guest: i, host: i, host_ip: "127.0.0.1"
  end
    
  # Map Postgres port from host to guest
  config.vm.network :forwarded_port, guest: 5432, host: 5432, host_ip: "127.0.0.1"
    
  # Reload machine to apply all pending changes
  config.vm.provision :reload
  
end