/**
 * Territory editor panel
 */

"use strict";

import { useState, useMemo, useRef, useEffect } from "react";

import Nodes from "nodes.js";
import * as UI from "ui/ui.jsx";
import IconDelete from "assets/icon/icon-x.svg";
import IconDeleteNode from "assets/icon/icon-x-thin.svg";
import IconPlus from "assets/icon/icon-plus.svg";
import IconMerge from "assets/icon/icon-terr-merge.svg";
import IconPaint from "assets/icon/icon-terr-paint.svg";

import "ui/css/nodes-scrollbar.css";
import "editor/css/panes/common.css";
import "editor/css/panes/nodes-pane.css";     // re-use nodes panel css for nodes list
import "editor/css/panes/territory-pane.css";

// ===============================
// territory nodes list helper
// ===============================
const TerritoryNodesList = (props) => {
    const nodesDivList = [];
    if ( props.selectedTerritory !== undefined ) {
        props.selectedTerritory.nodes.forEach( nodeName => {
            if ( props.nodes.has(nodeName) ) {
                let icon = props.nodes.get(nodeName).icon;
                let iconSrc = props.resourceIcons.get(icon);

                nodesDivList.push(
                    <div key={nodeName} className="nodes-editor-terr-nodes-list-item">
                        <div className="nodes-editor-terr-nodes-list-item-icon">
                            {iconSrc !== undefined ?
                            <img
                                className="nodes-editor-terr-nodes-list-item-img"
                                src={iconSrc}
                                draggable={false}
                            />
                            : (null)}
                        </div>
                        <div className="nodes-editor-terr-nodes-list-name">
                            {nodeName}
                        </div>
                        <div
                            className="nodes-editor-terr-nodes-list-item-delete"
                            onClick={() => props.removeNodeFromTerritory(props.selectedTerritory.id, nodeName)}
                        >
                            <img
                                className="nodes-editor-terr-nodes-list-item-x"
                                src={IconDeleteNode}
                                draggable={false}
                            />
                        </div>
                    </div>
                );
            }
        });
    }

    return (
        <UI.List
            id="nodes-editor-terr-list"
            list={props.selectedTerritoryNodes}
            selected={undefined}
            select={undefined}
            deselect={undefined}
            heightOfItem={20}
        >
            {nodesDivList}
        </UI.List>
    );
};

// ===============================
// Main Territory Pane
// ===============================
export const TerritoryPane = (props) => {

    const [inputNodeName, setInputNodeName] = useState("");
    const textAreaRef = useRef(null);
    
    const selectedTerritory = props.selectedTerritory;

    // Logic for Multi-Select ID List
    const selectedCount = Nodes.selectedTerritories.size;

    const selectedTerritoryListString = useMemo(() => {
        const ids = Array.from(Nodes.selectedTerritories.keys());
        return ids.length > 0 ? ids.join(" ") : "";
    }, [selectedCount, selectedTerritory]);

    // Auto-grow height logic
    useEffect(() => {
        if (textAreaRef.current) {
            textAreaRef.current.style.height = "auto";
            textAreaRef.current.style.height = textAreaRef.current.scrollHeight + "px";
        }
    }, [selectedTerritoryListString]);

    const handleCopy = () => {
        navigator.clipboard.writeText(selectedTerritoryListString);
        // Optional: You could trigger a small "Copied!" toast here
    };

    const handleAddNodeToTerritory = () => {
        if ( selectedTerritory !== undefined ) {
            let status = props.addNodeToTerritory(selectedTerritory.id, inputNodeName);
            if ( status === true ) {
                setInputNodeName("");
            }
        }
    };

    // territory info strings
    const selectedTerritoryName = selectedTerritory !== undefined ? selectedTerritory.name : "";
    const selectedTerritoryId = `ID: ${selectedTerritory !== undefined ? selectedTerritory.id : ""}`;
    const selectedTerritoryCore = `Core: ${selectedTerritory !== undefined && selectedTerritory.coreChunk ? `${selectedTerritory.coreChunk.x},${selectedTerritory.coreChunk.y}` : ""}`
    const selectedTerritorySize = `Chunks: ${selectedTerritory !== undefined ? selectedTerritory.size : ""}`;
    const selectedTerritoryCost = `Cost: ${selectedTerritory !== undefined ? selectedTerritory.cost : ""}`;
    const selectedTerritoryNodes = selectedTerritory !== undefined ? selectedTerritory.nodes : undefined;
    const selectedTerritoryNodesCount = selectedTerritoryNodes !== undefined ? selectedTerritoryNodes.length : 0;

    const territoryNodesList = useMemo(() => TerritoryNodesList({
        nodes: props.nodes,
        resourceIcons: props.resourceIcons,
        selectedTerritory: selectedTerritory,
        selectedTerritoryNodes: selectedTerritoryNodes,
        removeNodeFromTerritory: props.removeNodeFromTerritory,
    }), [selectedTerritoryNodesCount, selectedTerritory]);

    return (
        <>
        <div id="nodes-editor-terr-header">Territories:</div>

        <div id="nodes-editor-terr-chunk">
            <div id="nodes-editor-terr-chunk-label">Chunk:</div>
            <div>x: {props.x}</div>
            <div>z: {props.z}</div>
        </div>

        <div id="nodes-editor-terr-toolbar">
            <div id="nodes-editor-terr-toolbar-g1">
                <UI.Button
                    className="nodes-editor-terr-tool-btn"
                    onClick={props.createTerritory}
                    icon={IconPlus}
                    tooltip={"Create territory"}
                />
                <UI.Button
                    className="nodes-editor-terr-tool-btn"
                    onClick={() => props.deleteTerritory(props.selectedTerritory?.id)} 
                    icon={IconDelete}
                    tooltip={"Delete selected territory"}
                    disabled={!props.selectedTerritory}
                />
            </div>
            <div id="nodes-editor-terr-toolbar-g2">
                <UI.Button
                    className="nodes-editor-terr-tool-btn"
                    onClick={props.togglePainting}
                    icon={IconPaint}
                    tooltip={"Paint territory chunks"}
                />
                <UI.Button
                    className="nodes-editor-terr-tool-btn"
                    onClick={Nodes.mergeSelectedTerritories}
                    icon={IconMerge}
                    tooltip={"Merge territories"}
                />
            </div>
        </div>
        <div id="nodes-editor-brush-size">
            {`Brush Radius: ${props.paintRadius.toFixed(2)}`}
        </div>

        <div id="nodes-editor-terr-selected-header">Selected Territory Info:</div>
        <div id="nodes-editor-terr-selected-name">
            <div>Name:</div>
            <UI.InputEdit
                id="nodes-editor-terr-selected-name-edit"
                value={selectedTerritoryName}
                onChange={(newName) => props.setTerritoryName(selectedTerritory, newName)}
            />
        </div>
        <div>{selectedTerritoryId}</div>
        <div>{selectedTerritoryCore}</div>
        <div>{selectedTerritorySize}</div>
        <div>{selectedTerritoryCost}</div>
        
        <div style={{marginTop: "10px"}}>Nodes inside territory:</div>
        {territoryNodesList}

        <div id="nodes-editor-terr-add-node">
            <UI.Button
                className="nodes-editor-terr-tool-btn"
                onClick={handleAddNodeToTerritory}
                icon={IconPlus}
                tooltip={"Add resource node"}
            />
            <UI.InputEdit
                className="nodes-editor-terr-add-node-input"
                value={inputNodeName}
                bubbleChange={true}
                onChange={setInputNodeName}
                onEnterKey={handleAddNodeToTerritory}
            />
        </div>

        {/* --- MULTI-SELECT ID SECTION --- */}
        <hr style={{border: "0", borderTop: "1px solid #444", margin: "15px 0"}} />
        
        <div style={{display: "flex", justifyContent: "space-between", alignItems: "center"}}>
            <div id="nodes-editor-terr-selected-list-header">
                Multi-Selected IDs ({selectedCount}):
            </div>
            {selectedCount > 0 && (
                <div 
                    onClick={handleCopy}
                    style={{fontSize: "10px", color: "#aaa", cursor: "pointer", textDecoration: "underline"}}
                >
                    Copy All
                </div>
            )}
        </div>
        
        <textarea
            ref={textAreaRef}
            className="nodes-editor-terr-add-node-input"
            style={{
                width: "100%",
                minHeight: "40px",
                backgroundColor: "rgba(0,0,0,0.2)",
                color: "#fff",
                border: "1px solid #444",
                borderRadius: "3px",
                padding: "8px",
                fontSize: "13px",
                lineHeight: "1.4",
                fontFamily: "inherit",
                resize: "none",        /* Resize is now automatic */
                overflow: "hidden",    /* Hides scrollbar as it grows */
                wordWrap: "break-word",
                whiteSpace: "pre-wrap",
                outline: "none"
            }}
            value={selectedTerritoryListString}
            readOnly={true}
            placeholder="Right-click territories..."
            onChange={() => {}} 
        />
        
        <div style={{fontSize: "11px", color: "#666", marginTop: "6px", fontStyle: "italic"}}>
            Right-click multiple territories to collect IDs for bulk config.
        </div>
        
        {/* --- HELP SECTION --- */}
        <div className="nodes-editor-help" style={{marginTop: "20px"}}>
            <div>Help/Controls:</div>
            <div>- [Right click]: Select multiple territories</div>
            <div>- [Space bar]: Turn on/off paint mode</div>
            <div>- [Right mouse drag]: Paint chunks</div>
            <div>- [Ctrl + right mouse drag]: Erase chunks</div>
            <div>- [Shift + mouse drag]: Change brush size</div>
            <div>- [A]: While painting will create a new territory</div>
            <div>- [E]: Merge selected territories (buggy with resource nodes)</div>
            <div>- [R]: Distribute resources in selected territories</div>
            <div>- [X]: Subdivide selected territories into smaller ones</div>
            <div>- [Delete]: Delete selected territories</div>
        </div>
        </>
    );

};