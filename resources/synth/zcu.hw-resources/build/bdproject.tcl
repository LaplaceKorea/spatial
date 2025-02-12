if { $argc != 1 } {
  puts $argc
  puts [llength $argv]
  foreach i $argv {puts $i}
  puts "The second arg is [lindex $argv 1]"; #indexes start at 0
  puts "Usage: settings.tcl <clockFreqMHz>"
  exit -1
}

set CLOCK_FREQ_MHZ [lindex $argv 0]
set CLOCK_FREQ_HZ  [expr $CLOCK_FREQ_MHZ * 1000000]

source settings.tcl
set ver [version -short]

# Create project to make processing system IP from bd
create_project bd_project ./bd_project -part $PART
set_property board_part $BOARD [current_project]

add_files -norecurse [glob *.v]
add_files -norecurse [glob *.sv]
update_compile_order -fileset sources_1
update_compile_order -fileset sim_1

## Create processing system using bd
create_bd_design "design_1"
update_compile_order -fileset sources_1
switch $TARGET {
  "ZCU102" {
    set RST_FREQ [expr $CLOCK_FREQ_MHZ - 1]

    # Drop in ps-pl and Top (not sure if branching based on version is right..)
    if {[string match 2019* $ver]} {
        create_bd_cell -type ip -vlnv xilinx.com:ip:zynq_ultra_ps_e:3.3 zynq_ultra_ps_e_0
    # } elseif {[string match 2018* $ver]} {
    #     create_bd_cell -type ip -vlnv xilinx.com:ip:zynq_ultra_ps_e:3.2 zynq_ultra_ps_e_0
    } else {
        create_bd_cell -type ip -vlnv xilinx.com:ip:zynq_ultra_ps_e:3.0 zynq_ultra_ps_e_0
    }
    create_bd_cell -type ip -vlnv xilinx.com:ip:proc_sys_reset:5.0 rst_ps8_0_${RST_FREQ}M
    ## Set freqs
    set_property -dict [list CONFIG.PSU__FPGA_PL1_ENABLE {1} CONFIG.PSU__CRL_APB__PL1_REF_CTRL__SRCSEL {IOPLL} CONFIG.PSU__CRL_APB__PL1_REF_CTRL__FREQMHZ {250}] [get_bd_cells zynq_ultra_ps_e_0]
    set_property -dict [list CONFIG.PSU__CRL_APB__PL0_REF_CTRL__FREQMHZ $CLOCK_FREQ_MHZ] [get_bd_cells zynq_ultra_ps_e_0]
    apply_bd_automation -rule xilinx.com:bd_rule:zynq_ultra_ps_e -config {apply_board_preset "1" }  [get_bd_cells zynq_ultra_ps_e_0]
    create_bd_cell -type module -reference SpatialIP SpatialIP_0
    apply_bd_automation -rule xilinx.com:bd_rule:axi4 -config {Master "/zynq_ultra_ps_e_0/M_AXI_HPM0_FPD" Clk "/zynq_ultra_ps_e_0/pl_clk0 ($CLOCK_FREQ_MHZ MHz)" }  [get_bd_intf_pins SpatialIP_0/io_S_AXI]

    # DWIDTH converters
    create_bd_cell -type ip -vlnv xilinx.com:ip:axi_dwidth_converter:2.1 axi_dwidth_converter_0
    create_bd_cell -type ip -vlnv xilinx.com:ip:axi_dwidth_converter:2.1 axi_dwidth_converter_1

    connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/pl_clk0] [get_bd_pins zynq_ultra_ps_e_0/maxihpm1_fpd_aclk]

    # # Make AXI4 to AXI3 protocol converters
    # create_bd_cell -type ip -vlnv xilinx.com:ip:axi_protocol_converter:2.1 axi_protocol_converter_0
    # create_bd_cell -type ip -vlnv xilinx.com:ip:axi_protocol_converter:2.1 axi_protocol_converter_1
    # create_bd_cell -type ip -vlnv xilinx.com:ip:axi_protocol_converter:2.1 axi_protocol_converter_2
    # create_bd_cell -type ip -vlnv xilinx.com:ip:axi_protocol_converter:2.1 axi_protocol_converter_3

    # Disable unused interfaces
    set_property -dict [list CONFIG.PSU__PCIE__PERIPHERAL__ENABLE {0} CONFIG.PSU__DISPLAYPORT__PERIPHERAL__ENABLE {0}] [get_bd_cells zynq_ultra_ps_e_0]

    # Set axil s clock
    connect_bd_net [get_bd_pins SpatialIP_0/io_axil_s_clk] [get_bd_pins zynq_ultra_ps_e_0/pl_clk0]
    connect_bd_net [get_bd_pins axi_dwidth_converter_0/s_axi_aresetn] [get_bd_pins SpatialIP_0/reset]

    # # Enable EMIO reset
    # set_property -dict [list CONFIG.PSU__GPIO_EMIO__PERIPHERAL__ENABLE {1}] [get_bd_cells zynq_ultra_ps_e_0]

    # Use HP
    set_property -dict [list CONFIG.PSU__USE__S_AXI_GP2 {0} CONFIG.PSU__USE__S_AXI_GP3 {0} CONFIG.PSU__USE__S_AXI_GP4 {0} CONFIG.PSU__USE__S_AXI_GP5 {0}] [get_bd_cells zynq_ultra_ps_e_0]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxihp0_fpd_aclk] [get_bd_pins zynq_ultra_ps_e_0/pl_clk0]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxihp1_fpd_aclk] [get_bd_pins zynq_ultra_ps_e_0/pl_clk0]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxihp2_fpd_aclk] [get_bd_pins zynq_ultra_ps_e_0/pl_clk0]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxihp3_fpd_aclk] [get_bd_pins zynq_ultra_ps_e_0/pl_clk0]
    # connect_bd_intf_net [get_bd_intf_pins axi_dwidth_converter_0/M_AXI] [get_bd_intf_pins zynq_ultra_ps_e_0/S_AXI_HP0_FPD]
    # connect_bd_intf_net [get_bd_intf_pins axi_dwidth_converter_1/M_AXI] [get_bd_intf_pins zynq_ultra_ps_e_0/S_AXI_HP1_FPD]
    # Use ACP
    set_property -dict [list CONFIG.PSU__USE__S_AXI_ACP {0}] [get_bd_cells zynq_ultra_ps_e_0]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxiacp_fpd_aclk] [get_bd_pins zynq_ultra_ps_e_0/pl_clk0]
    # connect_bd_intf_net [get_bd_intf_pins axi_dwidth_converter_0/M_AXI] [get_bd_intf_pins zynq_ultra_ps_e_0/S_AXI_ACP_FPD]
    # Use HPC
    set_property -dict [list CONFIG.PSU__USE__S_AXI_GP0 {1} CONFIG.PSU__USE__S_AXI_GP1 {1}] [get_bd_cells zynq_ultra_ps_e_0]
    connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxihpc0_fpd_aclk] [get_bd_pins zynq_ultra_ps_e_0/pl_clk0]
    connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxihpc1_fpd_aclk] [get_bd_pins zynq_ultra_ps_e_0/pl_clk0]
    connect_bd_intf_net [get_bd_intf_pins axi_dwidth_converter_0/M_AXI] [get_bd_intf_pins zynq_ultra_ps_e_0/S_AXI_HPC0_FPD]
    connect_bd_intf_net [get_bd_intf_pins axi_dwidth_converter_1/M_AXI] [get_bd_intf_pins zynq_ultra_ps_e_0/S_AXI_HPC1_FPD]

    # Use kingston ddr4
    # set_property -dict [list CONFIG.SUBPRESET1 {DDR4_KINGSTON_KVR21SE15S8}] [get_bd_cells zynq_ultra_ps_e_0]
    # set_property -dict [list CONFIG.SUBPRESET1 {DDR4_MICRON_MT40A256M16GE_083E}] [get_bd_cells zynq_ultra_ps_e_0]
    # set_property -dict [list CONFIG.SUBPRESET1 {DDR4_SAMSUNG_K4A8G165WB_BCRC}] [get_bd_cells zynq_ultra_ps_e_0]

    # 512-to-64 data width converters
    # create_bd_cell -type ip -vlnv xilinx.com:ip:axi_dwidth_converter:2.1 axi_dwidth_converter_2
    # create_bd_cell -type ip -vlnv xilinx.com:ip:axi_dwidth_converter:2.1 axi_dwidth_converter_3
    set_property -dict [list CONFIG.SI_ID_WIDTH.VALUE_SRC USER CONFIG.SI_DATA_WIDTH.VALUE_SRC USER CONFIG.MI_DATA_WIDTH.VALUE_SRC USER CONFIG.READ_WRITE_MODE.VALUE_SRC USER CONFIG.PROTOCOL.VALUE_SRC USER CONFIG.ADDR_WIDTH.VALUE_SRC USER] [get_bd_cells axi_dwidth_converter_0]
    set_property -dict [list CONFIG.SI_ID_WIDTH.VALUE_SRC USER CONFIG.SI_DATA_WIDTH.VALUE_SRC USER CONFIG.MI_DATA_WIDTH.VALUE_SRC USER CONFIG.READ_WRITE_MODE.VALUE_SRC USER CONFIG.PROTOCOL.VALUE_SRC USER CONFIG.ADDR_WIDTH.VALUE_SRC USER] [get_bd_cells axi_dwidth_converter_1]
    # set_property -dict [list CONFIG.SI_ID_WIDTH.VALUE_SRC USER CONFIG.SI_DATA_WIDTH.VALUE_SRC USER CONFIG.MI_DATA_WIDTH.VALUE_SRC USER CONFIG.READ_WRITE_MODE.VALUE_SRC USER CONFIG.PROTOCOL.VALUE_SRC USER CONFIG.ADDR_WIDTH.VALUE_SRC USER] [get_bd_cells axi_dwidth_converter_2]
    # set_property -dict [list CONFIG.SI_ID_WIDTH.VALUE_SRC USER CONFIG.SI_DATA_WIDTH.VALUE_SRC USER CONFIG.MI_DATA_WIDTH.VALUE_SRC USER CONFIG.READ_WRITE_MODE.VALUE_SRC USER CONFIG.PROTOCOL.VALUE_SRC USER CONFIG.ADDR_WIDTH.VALUE_SRC USER] [get_bd_cells axi_dwidth_converter_3]
    set_property -dict [list CONFIG.SI_DATA_WIDTH {512} CONFIG.SI_ID_WIDTH {32} CONFIG.MI_DATA_WIDTH {128}] [get_bd_cells axi_dwidth_converter_0]
    set_property -dict [list CONFIG.SI_DATA_WIDTH {512} CONFIG.SI_ID_WIDTH {32} CONFIG.MI_DATA_WIDTH {128}] [get_bd_cells axi_dwidth_converter_1]
    # set_property -dict [list CONFIG.SI_DATA_WIDTH {512} CONFIG.SI_ID_WIDTH {32} CONFIG.MI_DATA_WIDTH {128}] [get_bd_cells axi_dwidth_converter_2]
    # set_property -dict [list CONFIG.SI_DATA_WIDTH {512} CONFIG.SI_ID_WIDTH {32} CONFIG.MI_DATA_WIDTH {128}] [get_bd_cells axi_dwidth_converter_3]
    connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/pl_clk0] [get_bd_pins axi_dwidth_converter_0/s_axi_aclk]
    connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/pl_clk0] [get_bd_pins axi_dwidth_converter_1/s_axi_aclk]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/pl_clk0] [get_bd_pins axi_dwidth_converter_2/s_axi_aclk]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/pl_clk0] [get_bd_pins axi_dwidth_converter_3/s_axi_aclk]
    connect_bd_net [get_bd_pins rst_ps8_0_${RST_FREQ}M/peripheral_aresetn] [get_bd_pins axi_dwidth_converter_0/s_axi_aresetn]
    connect_bd_net [get_bd_pins rst_ps8_0_${RST_FREQ}M/peripheral_aresetn] [get_bd_pins axi_dwidth_converter_1/s_axi_aresetn]
    # connect_bd_net [get_bd_pins rst_ps8_0_${RST_FREQ}M/peripheral_aresetn] [get_bd_pins axi_dwidth_converter_2/s_axi_aresetn]
    # connect_bd_net [get_bd_pins rst_ps8_0_${RST_FREQ}M/peripheral_aresetn] [get_bd_pins axi_dwidth_converter_3/s_axi_aresetn]

    # # AXI4 to AXI3 protocol converter
    # set_property -dict [list CONFIG.MI_PROTOCOL.VALUE_SRC USER CONFIG.ID_WIDTH.VALUE_SRC USER CONFIG.DATA_WIDTH.VALUE_SRC USER CONFIG.SI_PROTOCOL.VALUE_SRC USER] [get_bd_cells axi_protocol_converter_0]
    # set_property -dict [list CONFIG.MI_PROTOCOL {AXI3} CONFIG.DATA_WIDTH {64} CONFIG.ID_WIDTH {6} CONFIG.TRANSLATION_MODE {2}] [get_bd_cells axi_protocol_converter_0]
    # connect_bd_net [get_bd_pins proc_sys_reset_fclk1/slowest_sync_clk] [get_bd_pins zynq_ultra_ps_e_0/pl_clk0]
    # connect_bd_net [get_bd_pins axi_protocol_converter_0/aclk] [get_bd_pins zynq_ultra_ps_e_0/pl_clk0]
    # connect_bd_net [get_bd_pins axi_protocol_converter_1/aclk] [get_bd_pins zynq_ultra_ps_e_0/pl_clk0]
    # connect_bd_net [get_bd_pins axi_protocol_converter_2/aclk] [get_bd_pins zynq_ultra_ps_e_0/pl_clk0]
    # connect_bd_net [get_bd_pins axi_protocol_converter_3/aclk] [get_bd_pins zynq_ultra_ps_e_0/pl_clk0]
    # connect_bd_net [get_bd_pins axi_protocol_converter_0/aresetn] [get_bd_pins rst_ps8_0_${RST_FREQ}M/peripheral_aresetn]
    # connect_bd_net [get_bd_pins axi_protocol_converter_1/aresetn] [get_bd_pins rst_ps8_0_${RST_FREQ}M/peripheral_aresetn]
    # connect_bd_net [get_bd_pins axi_protocol_converter_2/aresetn] [get_bd_pins rst_ps8_0_${RST_FREQ}M/peripheral_aresetn]
    # connect_bd_net [get_bd_pins axi_protocol_converter_3/aresetn] [get_bd_pins rst_ps8_0_${RST_FREQ}M/peripheral_aresetn]
    # # Clock converters from FCLKCLK0 <-> FCLKCLK1
    # create_bd_cell -type ip -vlnv xilinx.com:ip:axi_clock_converter:2.1 axi_clock_converter_0
    # create_bd_cell -type ip -vlnv xilinx.com:ip:axi_clock_converter:2.1 axi_clock_converter_1
    # create_bd_cell -type ip -vlnv xilinx.com:ip:axi_clock_converter:2.1 axi_clock_converter_2
    # create_bd_cell -type ip -vlnv xilinx.com:ip:axi_clock_converter:2.1 axi_clock_converter_3
    # set_property -dict [list CONFIG.ID_WIDTH.VALUE_SRC USER CONFIG.DATA_WIDTH.VALUE_SRC USER CONFIG.READ_WRITE_MODE.VALUE_SRC USER CONFIG.ADDR_WIDTH.VALUE_SRC USER CONFIG.PROTOCOL.VALUE_SRC USER] [get_bd_cells axi_clock_converter_0]
    # set_property -dict [list CONFIG.ID_WIDTH.VALUE_SRC USER CONFIG.DATA_WIDTH.VALUE_SRC USER CONFIG.READ_WRITE_MODE.VALUE_SRC USER CONFIG.ADDR_WIDTH.VALUE_SRC USER CONFIG.PROTOCOL.VALUE_SRC USER] [get_bd_cells axi_clock_converter_1]
    # set_property -dict [list CONFIG.ID_WIDTH.VALUE_SRC USER CONFIG.DATA_WIDTH.VALUE_SRC USER CONFIG.READ_WRITE_MODE.VALUE_SRC USER CONFIG.ADDR_WIDTH.VALUE_SRC USER CONFIG.PROTOCOL.VALUE_SRC USER] [get_bd_cells axi_clock_converter_2]
    # set_property -dict [list CONFIG.ID_WIDTH.VALUE_SRC USER CONFIG.DATA_WIDTH.VALUE_SRC USER CONFIG.READ_WRITE_MODE.VALUE_SRC USER CONFIG.ADDR_WIDTH.VALUE_SRC USER CONFIG.PROTOCOL.VALUE_SRC USER] [get_bd_cells axi_clock_converter_3]
    # set_property -dict [list CONFIG.PROTOCOL {AXI4} CONFIG.DATA_WIDTH {128} CONFIG.ID_WIDTH {6}] [get_bd_cells axi_clock_converter_0]
    # set_property -dict [list CONFIG.PROTOCOL {AXI4} CONFIG.DATA_WIDTH {128} CONFIG.ID_WIDTH {6}] [get_bd_cells axi_clock_converter_1]
    # set_property -dict [list CONFIG.PROTOCOL {AXI4} CONFIG.DATA_WIDTH {128} CONFIG.ID_WIDTH {6}] [get_bd_cells axi_clock_converter_2]
    # set_property -dict [list CONFIG.PROTOCOL {AXI4} CONFIG.DATA_WIDTH {128} CONFIG.ID_WIDTH {6}] [get_bd_cells axi_clock_converter_3]

    # # Loopback debuggers
    # # top signals
    # connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_ARVALID] [get_bd_pins SpatialIP_0/io_TOP_AXI_ARVALID]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/s_axi_arvalid] [get_bd_pins SpatialIP_0/io_M_AXI_0_ARVALID]
    # connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_ARREADY] [get_bd_pins SpatialIP_0/io_TOP_AXI_ARREADY]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/s_axi_arready] [get_bd_pins SpatialIP_0/io_M_AXI_0_ARREADY]
    # connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_ARADDR] [get_bd_pins SpatialIP_0/io_TOP_AXI_ARADDR]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/s_axi_araddr] [get_bd_pins SpatialIP_0/io_M_AXI_0_ARADDR]
    # connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_ARLEN] [get_bd_pins SpatialIP_0/io_TOP_AXI_ARLEN]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/s_axi_arlen] [get_bd_pins SpatialIP_0/io_M_AXI_0_ARLEN]
    # connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_ARSIZE] [get_bd_pins SpatialIP_0/io_TOP_AXI_ARSIZE]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/s_axi_arsize] [get_bd_pins SpatialIP_0/io_M_AXI_0_ARSIZE]
    # connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_ARID] [get_bd_pins SpatialIP_0/io_TOP_AXI_ARID]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/s_axi_arid] [get_bd_pins SpatialIP_0/io_M_AXI_0_ARID]
    # connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_ARBURST] [get_bd_pins SpatialIP_0/io_TOP_AXI_ARBURST]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/s_axi_arburst] [get_bd_pins SpatialIP_0/io_M_AXI_0_ARBURST]
    # connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_ARLOCK] [get_bd_pins SpatialIP_0/io_TOP_AXI_ARLOCK]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/s_axi_arlock] [get_bd_pins SpatialIP_0/io_M_AXI_0_ARLOCK]
    #
    # connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_AWVALID] [get_bd_pins SpatialIP_0/io_TOP_AXI_AWVALID]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/s_axi_awvalid] [get_bd_pins SpatialIP_0/io_M_AXI_0_AWVALID]
    # connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_AWREADY] [get_bd_pins SpatialIP_0/io_TOP_AXI_AWREADY]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/s_axi_awready] [get_bd_pins SpatialIP_0/io_M_AXI_0_AWREADY]
    # connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_AWADDR] [get_bd_pins SpatialIP_0/io_TOP_AXI_AWADDR]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/s_axi_awaddr] [get_bd_pins SpatialIP_0/io_M_AXI_0_AWADDR]
    # connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_AWLEN] [get_bd_pins SpatialIP_0/io_TOP_AXI_AWLEN]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/s_axi_awlen] [get_bd_pins SpatialIP_0/io_M_AXI_0_AWLEN]
    #
    # connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_RVALID] [get_bd_pins SpatialIP_0/io_TOP_AXI_RVALID]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/s_axi_rvalid] [get_bd_pins SpatialIP_0/io_M_AXI_0_RVALID]
    # connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_RREADY] [get_bd_pins SpatialIP_0/io_TOP_AXI_RREADY]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/s_axi_rready] [get_bd_pins SpatialIP_0/io_M_AXI_0_RREADY]
    #
    # connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_WVALID] [get_bd_pins SpatialIP_0/io_TOP_AXI_WVALID]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/s_axi_wvalid] [get_bd_pins SpatialIP_0/io_M_AXI_0_WVALID]
    # connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_WREADY] [get_bd_pins SpatialIP_0/io_TOP_AXI_WREADY]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/s_axi_wready] [get_bd_pins SpatialIP_0/io_M_AXI_0_WREADY]
    # connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_WDATA] [get_bd_pins SpatialIP_0/io_TOP_AXI_WDATA]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/s_axi_wdata] [get_bd_pins SpatialIP_0/io_M_AXI_0_WDATA]
    # connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_WSTRB] [get_bd_pins SpatialIP_0/io_TOP_AXI_WSTRB]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/s_axi_wstrb] [get_bd_pins SpatialIP_0/io_M_AXI_0_WSTRB]
    #
    # connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_BVALID] [get_bd_pins SpatialIP_0/io_TOP_AXI_BVALID]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/s_axi_bvalid] [get_bd_pins SpatialIP_0/io_M_AXI_0_BVALID]
    # connect_bd_net [get_bd_pins SpatialIP_0/io_M_AXI_0_BREADY] [get_bd_pins SpatialIP_0/io_TOP_AXI_BREADY]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/s_axi_bready] [get_bd_pins SpatialIP_0/io_M_AXI_0_BREADY]
    #
    # # dwidth signals
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_arvalid] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_ARVALID]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_arready] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_ARREADY]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_araddr] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_ARADDR]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_arlen] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_ARLEN]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_arsize] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_ARSIZE]
    # # connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_arid] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_ARID]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_arburst] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_ARBURST]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_arlock] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_ARLOCK]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_awvalid] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_AWVALID]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_awready] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_AWREADY]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_awaddr] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_AWADDR]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_awlen] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_AWLEN]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_rvalid] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_RVALID]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_rready] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_RREADY]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_wvalid] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_WVALID]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_wready] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_WREADY]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_wdata] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_WDATA]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_wstrb] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_WSTRB]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_bvalid] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_BVALID]
    # connect_bd_net [get_bd_pins axi_dwidth_converter_0/m_axi_bready] [get_bd_pins SpatialIP_0/io_DWIDTH_AXI_BREADY]
    #
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_arvalid] [get_bd_pins axi_dwidth_converter_0/m_axi_arvalid]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_arready] [get_bd_pins axi_dwidth_converter_0/m_axi_arready]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_araddr] [get_bd_pins axi_dwidth_converter_0/m_axi_araddr]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_arlen] [get_bd_pins axi_dwidth_converter_0/m_axi_arlen]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_arsize] [get_bd_pins axi_dwidth_converter_0/m_axi_arsize]
    # # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_arid] [get_bd_pins axi_dwidth_converter_0/m_axi_arid]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_arburst] [get_bd_pins axi_dwidth_converter_0/m_axi_arburst]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_arlock] [get_bd_pins axi_dwidth_converter_0/m_axi_arlock]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_awvalid] [get_bd_pins axi_dwidth_converter_0/m_axi_awvalid]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_awready] [get_bd_pins axi_dwidth_converter_0/m_axi_awready]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_awaddr] [get_bd_pins axi_dwidth_converter_0/m_axi_awaddr]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_awlen] [get_bd_pins axi_dwidth_converter_0/m_axi_awlen]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_rvalid] [get_bd_pins axi_dwidth_converter_0/m_axi_rvalid]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_rready] [get_bd_pins axi_dwidth_converter_0/m_axi_rready]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_wvalid] [get_bd_pins axi_dwidth_converter_0/m_axi_wvalid]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_wready] [get_bd_pins axi_dwidth_converter_0/m_axi_wready]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_wdata] [get_bd_pins axi_dwidth_converter_0/m_axi_wdata]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_wstrb] [get_bd_pins axi_dwidth_converter_0/m_axi_wstrb]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_bvalid] [get_bd_pins axi_dwidth_converter_0/m_axi_bvalid]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_bready] [get_bd_pins axi_dwidth_converter_0/m_axi_bready]

    # # clockconverter signals
    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_arvalid] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_ARVALID]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_arvalid] [get_bd_pins axi_clock_converter_0/m_axi_arvalid]
    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_arready] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_ARREADY]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_arready] [get_bd_pins axi_clock_converter_0/m_axi_arready]
    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_araddr] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_ARADDR]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_araddr] [get_bd_pins axi_clock_converter_0/m_axi_araddr]
    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_arlen] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_ARLEN]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_arlen] [get_bd_pins axi_clock_converter_0/m_axi_arlen]
    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_arsize] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_ARSIZE]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_arsize] [get_bd_pins axi_clock_converter_0/m_axi_arsize]
    # # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_arid] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_ARID]
    # # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_arid] [get_bd_pins axi_clock_converter_0/m_axi_arid]
    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_arburst] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_ARBURST]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_arburst] [get_bd_pins axi_clock_converter_0/m_axi_arburst]
    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_arlock] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_ARLOCK]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_arlock] [get_bd_pins axi_clock_converter_0/m_axi_arlock]


    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_awvalid] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_AWVALID]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_awvalid] [get_bd_pins axi_clock_converter_0/m_axi_awvalid]
    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_awready] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_AWREADY]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_awready] [get_bd_pins axi_clock_converter_0/m_axi_awready]
    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_awaddr] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_AWADDR]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_awaddr] [get_bd_pins axi_clock_converter_0/m_axi_awaddr]

    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_rvalid] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_RVALID]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_rvalid] [get_bd_pins axi_clock_converter_0/m_axi_rvalid]
    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_rready] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_RREADY]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_rready] [get_bd_pins axi_clock_converter_0/m_axi_rready]

    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_wvalid] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_WVALID]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_wvalid] [get_bd_pins axi_clock_converter_0/m_axi_wvalid]
    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_wready] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_WREADY]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_wready] [get_bd_pins axi_clock_converter_0/m_axi_wready]

    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_bvalid] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_BVALID]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_bvalid] [get_bd_pins axi_clock_converter_0/m_axi_bvalid]
    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_bready] [get_bd_pins SpatialIP_0/io_CLOCKCONVERT_AXI_BREADY]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/saxigp0_bready] [get_bd_pins axi_clock_converter_0/m_axi_bready]


    # # top -> dwidth converter
    connect_bd_intf_net [get_bd_intf_pins SpatialIP_0/io_M_AXI_0] [get_bd_intf_pins axi_dwidth_converter_0/S_AXI]
    connect_bd_intf_net [get_bd_intf_pins SpatialIP_0/io_M_AXI_1] [get_bd_intf_pins axi_dwidth_converter_1/S_AXI]
    # connect_bd_intf_net [get_bd_intf_pins SpatialIP_0/io_M_AXI_2] [get_bd_intf_pins axi_dwidth_converter_2/S_AXI]
    # connect_bd_intf_net [get_bd_intf_pins SpatialIP_0/io_M_AXI_3] [get_bd_intf_pins axi_dwidth_converter_3/S_AXI]
    # CLOCK CROSSING HACK
    # # data width converter -> clock converter
    # connect_bd_intf_net [get_bd_intf_pins axi_dwidth_converter_0/M_AXI] [get_bd_intf_pins axi_clock_converter_0/S_AXI]
    # connect_bd_intf_net [get_bd_intf_pins axi_dwidth_converter_1/M_AXI] [get_bd_intf_pins axi_clock_converter_1/S_AXI]
    # connect_bd_intf_net [get_bd_intf_pins axi_dwidth_converter_2/M_AXI] [get_bd_intf_pins axi_clock_converter_2/S_AXI]
    # connect_bd_intf_net [get_bd_intf_pins axi_dwidth_converter_3/M_AXI] [get_bd_intf_pins axi_clock_converter_3/S_AXI]
    # # clock converter -> Top
    # connect_bd_intf_net [get_bd_intf_pins axi_clock_converter_0/M_AXI] [get_bd_intf_pins zynq_ultra_ps_e_0/S_AXI_HP0_FPD]
    # connect_bd_intf_net [get_bd_intf_pins axi_clock_converter_1/M_AXI] [get_bd_intf_pins zynq_ultra_ps_e_0/S_AXI_HP1_FPD]
    # connect_bd_intf_net [get_bd_intf_pins axi_clock_converter_2/M_AXI] [get_bd_intf_pins zynq_ultra_ps_e_0/S_AXI_HP2_FPD]
    # connect_bd_intf_net [get_bd_intf_pins axi_clock_converter_3/M_AXI] [get_bd_intf_pins zynq_ultra_ps_e_0/S_AXI_HP3_FPD]


    # Wire up the resets
    # connect_bd_net [get_bd_pins axi_clock_converter_0/s_axi_aresetn] [get_bd_pins axi_clock_converter_0/m_axi_aresetn]
    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_aresetn] [get_bd_pins axi_clock_converter_1/s_axi_aresetn]
    # connect_bd_net [get_bd_pins axi_clock_converter_1/s_axi_aresetn] [get_bd_pins axi_clock_converter_1/m_axi_aresetn]
    # connect_bd_net [get_bd_pins axi_clock_converter_1/m_axi_aresetn] [get_bd_pins axi_clock_converter_2/s_axi_aresetn]
    # connect_bd_net [get_bd_pins axi_clock_converter_2/s_axi_aresetn] [get_bd_pins axi_clock_converter_2/m_axi_aresetn]
    # connect_bd_net [get_bd_pins axi_clock_converter_2/m_axi_aresetn] [get_bd_pins axi_clock_converter_3/s_axi_aresetn]
    # connect_bd_net [get_bd_pins axi_clock_converter_3/s_axi_aresetn] [get_bd_pins axi_clock_converter_3/m_axi_aresetn]
    # connect_bd_net [get_bd_pins axi_clock_converter_0/m_axi_aresetn] [get_bd_pins rst_ps8_0_${RST_FREQ}M/peripheral_aresetn]

    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/pl_clk0] [get_bd_pins axi_clock_converter_0/s_axi_aclk]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/pl_clk0] [get_bd_pins axi_clock_converter_1/s_axi_aclk]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/pl_clk0] [get_bd_pins axi_clock_converter_2/s_axi_aclk]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/pl_clk0] [get_bd_pins axi_clock_converter_3/s_axi_aclk]
    ## CLOCK CROSSING HACK
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/pl_clk1] [get_bd_pins axi_clock_converter_0/m_axi_aclk]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/pl_clk1] [get_bd_pins axi_clock_converter_1/m_axi_aclk]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/pl_clk1] [get_bd_pins axi_clock_converter_2/m_axi_aclk]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/pl_clk1] [get_bd_pins axi_clock_converter_3/m_axi_aclk]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/pl_clk0] [get_bd_pins axi_clock_converter_0/m_axi_aclk]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/pl_clk0] [get_bd_pins axi_clock_converter_1/m_axi_aclk]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/pl_clk0] [get_bd_pins axi_clock_converter_2/m_axi_aclk]
    # connect_bd_net [get_bd_pins zynq_ultra_ps_e_0/pl_clk0] [get_bd_pins axi_clock_converter_3/m_axi_aclk]
    # what to do about Clock converter -> HP0?
    # what to do about Address assignment to HP0?

    # set_property DONT_TOUCH true [get_cells /SpatialIP_0]

  }
  default {
  }
}

validate_bd_design
save_bd_design

make_wrapper -files [get_files ./bd_project/bd_project.srcs/sources_1/bd/design_1/design_1.bd] -top
add_files -norecurse ./bd_project/bd_project.srcs/sources_1/bd/design_1/hdl/design_1_wrapper.v
update_compile_order -fileset sources_1

set_property top design_1_wrapper [current_fileset]
update_compile_order -fileset sources_1

# Copy required files here
if { [file exists ./bd_project/bd_project.srcs/sources_1/bd/design_1/hdl/design_1.v] == 1} {
    file copy -force ./bd_project/bd_project.srcs/sources_1/bd/design_1/hdl/design_1.v ./design_1.v
} else {
    file copy -force ./bd_project/bd_project.srcs/sources_1/bd/design_1/sim/design_1.v ./design_1.v
}


file copy -force ./bd_project/bd_project.srcs/sources_1/bd/design_1/hdl/design_1.v ./design_1.v
file copy -force ./bd_project/bd_project.srcs/sources_1/bd/design_1/hdl/design_1_wrapper.v ./design_1_wrapper.v

close_project
