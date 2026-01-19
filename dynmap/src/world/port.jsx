export const Port = (props) => {
    let getPoint = props.getPoint;

    // coordinate
    const center = getPoint(props.x, props.z);

    // get icon size and apply size constraint 
    const iconSizeFromMapScale = getPoint(16, 0).x - getPoint(0, 0).x;
    let iconSize = 20;
    if (iconSizeFromMapScale === 4) {
        iconSize = 20;
    } else if (iconSizeFromMapScale === 8) {
        iconSize = 20;
    } else if (iconSizeFromMapScale === 16) {
        iconSize = 32;
    } else if (iconSizeFromMapScale === 32) {
        iconSize = 48;
    } else if (iconSizeFromMapScale > 32) {
        iconSize = 64;
    }

    const svgRef = useRef(null);

    /**
     * Call tooltip render
     */
    const showTooltip = () => {
        if (svgRef.current !== null) {
            const rect = svgRef.current.getBoundingClientRect();
            props.showPortTooltip(props.port, rect.x, rect.y);
        }
    };

    const cx = center.x - iconSize / 2;
    const cy = center.y - iconSize / 2;

    // Check if port is visible
    if (!props.portVisible) {
        return null; // Do not render anything if portVisible is false
    }

    return (
        <><g ref={svgRef} onMouseEnter={showTooltip} onMouseLeave={props.removePortTooltip}></g><image key={props.name} x={cx} y={cy} width={iconSize} height={iconSize} href={AnchorIcon} /></>
    );
}

export const PortTooltip = (props) => {
    let getPoint = props.getPoint;
    let style = {
        "left": props.clientX + 50,
        "top": props.clientY - 30,
    };

    const port = props.port;

    // Check if tooltip should be enabled
    if (!props.enable || !props.portVisible) {
        return null; // Do not render tooltip if not enabled or portVisible is false
    }

    return (
        <div
            id="port-tooltip"
            style={style}
        >
            <div><b>Port:</b> {port.name}</div>
            <div><b>Groups:</b> {port.groupsString}</div>
            <div><b>x:</b> {port.x}</div>
            <div><b>z:</b> {port.z}</div>
            {/* <div><b>Owner:</b> {port.owner} ?</div> */}
        </div>
    );
}
